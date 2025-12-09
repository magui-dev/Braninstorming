"""
브레인스토밍 API 엔드포인트 (JSON 기반 Ephemeral RAG)

아이디어 생성 워크플로우:
1. POST /session - 세션 시작
2. POST /purpose - Q1 목적 입력
3. GET /warmup/{session_id} - Q2 워밍업 질문 생성
4. POST /confirm/{session_id} - Q2 확인
5. POST /associations/{session_id} - Q3 자유연상 입력
6. GET /ideas/{session_id} - 아이디어 생성 및 분석
7. DELETE /session/{session_id} - 세션 삭제

변경사항 (2024-12-01):
- Ephemeral RAG: ChromaDB → JSON 기반으로 완전 전환
- 로깅 강화: 삭제/정리 작업 시 상세 로그
- Retry 로직 추가
- Dependencies 패턴 적용
"""

from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Dict
import sys
from pathlib import Path
import shutil
import logging
import time

# 로거 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 브레인스토밍 모듈 경로 추가
brainstorming_path = Path(__file__).resolve().parent.parent.parent.parent / "domain" / "brainstorming"
sys.path.insert(0, str(brainstorming_path))

from session_manager import SessionManager
from ephemeral_rag import EphemeralRAG
from domain_hints import get_domain_hint, format_hint_for_prompt

# 트렌드 검색 모듈 import
from search.naver_news import NaverNewsSearcher
from search.duckduckgo import DuckDuckGoSearcher
from search.naver_datalab import NaverDataLabSearcher

# ChromaDB import (영구 RAG 전용)
import chromadb
from chromadb.config import Settings as ChromaSettings
from openai import OpenAI
from dotenv import load_dotenv
import os

# 헬퍼 함수 import
from .utils.llm_helpers import call_llm_with_retry
from .dependencies import get_session_or_404, session_manager

load_dotenv()

router = APIRouter()

# 전역 인스턴스
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
llm_model = os.getenv("LLM_MODEL", "gpt-4o")
embedding_model = os.getenv("EMBEDDING_MODEL", "text-embedding-3-large")

# ============================================================
# 영구 RAG ChromaDB 클라이언트 (브레인스토밍 기법만!)
# ============================================================
module_dir = brainstorming_path
persist_directory = str(module_dir / "data" / "chroma")

chroma_client = chromadb.PersistentClient(
    path=persist_directory,
    settings=ChromaSettings(anonymized_telemetry=False)
)

try:
    permanent_collection = chroma_client.get_collection(
        name="brainstorming_techniques"
    )
    logger.info("✅ 영구 RAG 컬렉션 로드 완료 (brainstorming API)")
    logger.info(f"   📁 경로: {persist_directory}")
    logger.info(f"   📊 문서 수: {permanent_collection.count()}개")
except Exception as e:
    logger.warning(f"⚠️  영구 RAG 컬렉션 로드 실패: {e}")
    permanent_collection = None

# ============================================================
# 트렌드 검색기 초기화 (네이버 뉴스, DuckDuckGo, 네이버 데이터랩)
# ============================================================
trend_searcher = None
duckduckgo_searcher = None
datalab_searcher = None

try:
    trend_searcher = NaverNewsSearcher()
    logger.info("✅ 네이버 뉴스 트렌드 검색 초기화 완료")
except Exception as e:
    logger.warning(f"⚠️  네이버 뉴스 트렌드 검색 초기화 실패: {e}")

try:
    duckduckgo_searcher = DuckDuckGoSearcher()
    logger.info("✅ DuckDuckGo 트렌드 검색 초기화 완료")
except Exception as e:
    logger.warning(f"⚠️  DuckDuckGo 트렌드 검색 초기화 실패: {e}")

try:
    datalab_searcher = NaverDataLabSearcher()
    logger.info("✅ 네이버 데이터랩 트렌드 검색 초기화 완료")
except Exception as e:
    logger.warning(f"⚠️  네이버 데이터랩 트렌드 검색 초기화 실패: {e}")


# === Pydantic 모델 ===

class SessionResponse(BaseModel):
    """세션 생성 응답"""
    session_id: str
    message: str


class PurposeRequest(BaseModel):
    """Q1 목적 입력 요청"""
    session_id: str
    purpose: str


class PurposeResponse(BaseModel):
    """Q1 목적 입력 응답"""
    message: str
    purpose: str


class WarmupResponse(BaseModel):
    """Q2 워밍업 질문 응답"""
    questions: List[str]


class ConfirmResponse(BaseModel):
    """Q2 확인 응답"""
    message: str


class AssociationsRequest(BaseModel):
    """Q3 자유연상 입력 요청"""
    session_id: str
    associations: List[str]


class AssociationsResponse(BaseModel):
    """Q3 자유연상 입력 응답"""
    message: str
    count: int


class IdeaResponse(BaseModel):
    """아이디어 생성 응답"""
    ideas: List[Dict[str, str]]  # [{"title": "...", "description": "...", "analysis": "..."}]


class DeleteResponse(BaseModel):
    """세션 삭제 응답"""
    message: str


# === 트렌드 검색 헬퍼 함수 ===

async def fetch_trend_keywords(purpose: str) -> List[str]:
    """
    트렌드 키워드 검색 (네이버 뉴스 + DuckDuckGo + 네이버 데이터랩)
    
    Args:
        purpose: Q1 목적
        
    Returns:
        List[str]: 트렌드 키워드 리스트
    """
    all_keywords = []
    
    # 1. 네이버 뉴스 검색
    if trend_searcher:
        try:
            logger.info("   🔍 네이버 뉴스 트렌드 검색 중...")
            naver_keywords = await trend_searcher.extract_trend_keywords(purpose, num_articles=5)
            if naver_keywords:
                logger.info(f"      ✅ 네이버 뉴스: {len(naver_keywords)}개 발견")
                all_keywords.extend(naver_keywords)
        except Exception as e:
            logger.warning(f"      ⚠️  네이버 뉴스 검색 실패: {e}")
    
    # 2. DuckDuckGo 검색 (글로벌)
    if duckduckgo_searcher:
        try:
            logger.info("   🔍 DuckDuckGo 글로벌 트렌드 검색 중...")
            ddg_keywords = await duckduckgo_searcher.extract_trend_keywords(purpose, num_articles=5)
            if ddg_keywords:
                logger.info(f"      ✅ DuckDuckGo: {len(ddg_keywords)}개 발견")
                all_keywords.extend(ddg_keywords)
        except Exception as e:
            logger.warning(f"      ⚠️  DuckDuckGo 검색 실패: {e}")
    
    # 3. 네이버 데이터랩 검색
    if datalab_searcher:
        try:
            logger.info("   🔍 네이버 데이터랩 트렌드 검색 중...")
            datalab_keywords = await datalab_searcher.extract_trend_keywords(purpose)
            if datalab_keywords:
                logger.info(f"      ✅ 네이버 데이터랩: {len(datalab_keywords)}개 발견")
                all_keywords.extend(datalab_keywords)
        except Exception as e:
            logger.warning(f"      ⚠️  네이버 데이터랩 검색 실패: {e}")
    
    # 4. 중복 제거
    unique_keywords = list(dict.fromkeys(all_keywords))
    
    if unique_keywords:
        logger.info(f"   ✅ 총 트렌드 키워드 {len(unique_keywords)}개: {unique_keywords[:10]}")
    else:
        logger.info("   ℹ️  트렌드 키워드 없음")
    
    return unique_keywords


# === API 엔드포인트 ===

@router.post("/session", response_model=SessionResponse)
async def create_session():
    """
    새로운 브레인스토밍 세션 시작
    
    시작 전에 오래된 Ephemeral 세션 폴더를 자동으로 청소합니다.
    
    Returns:
        SessionResponse: 세션 ID와 메시지
    """
    try:
        # 🧹 1. 오래된 세션 폴더 자동 정리 (5분 이상)
        logger.info("🧹 오래된 Ephemeral 세션 폴더 청소 시작...")
        
        ephemeral_base_dir = Path(__file__).resolve().parent.parent.parent.parent / "domain" / "brainstorming" / "data" / "ephemeral"
        
        if ephemeral_base_dir.exists():
            deleted_count = 0
            current_time = time.time()
            cutoff_time = current_time - 300  # 5분 = 300초
            
            for session_dir in ephemeral_base_dir.iterdir():
                if not session_dir.is_dir():
                    continue
                
                # 폴더 수정 시간 확인
                mtime = session_dir.stat().st_mtime
                
                # 5분 이상 된 폴더만 확인
                if mtime < cutoff_time:
                    # 폴더가 비어있는지 확인
                    files = list(session_dir.iterdir())
                    
                    if len(files) == 0:  # 빈 폴더만 삭제
                        try:
                            shutil.rmtree(session_dir)
                            deleted_count += 1
                            logger.info(f"   - 삭제: {session_dir.name[:8]}... (생성 후 {(current_time - mtime)/60:.1f}분 경과)")
                        except Exception as e:
                            logger.warning(f"   - 삭제 실패: {session_dir.name[:8]}... ({e})")
            
            if deleted_count > 0:
                logger.info(f"✅ {deleted_count}개의 오래된 빈 폴더 삭제됨")
            else:
                logger.info("   ℹ️  삭제할 오래된 세션 없음")
        else:
            logger.warning(f"   ⚠️  Ephemeral 디렉토리 없음: {ephemeral_base_dir}")
        
        # 2. 새 세션 생성
        session_id = session_manager.create_session()
        logger.info(f"✅ 새 세션 생성: {session_id}")
        
        return SessionResponse(
            session_id=session_id,
            message="새로운 브레인스토밍 세션이 시작되었습니다."
        )
    except Exception as e:
        logger.error(f"❌ 세션 생성 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"세션 생성 실패: {str(e)}")


@router.post("/purpose", response_model=PurposeResponse)
async def submit_purpose(request: PurposeRequest):
    """
    Q1: 목적/도메인 입력
    
    Args:
        request: 세션 ID와 목적
        
    Returns:
        PurposeResponse: 확인 메시지
    """
    try:
        # 세션 존재 여부 확인
        session = get_session_or_404(request.session_id)
        
        # 세션에 목적 저장
        session_manager.update_session(request.session_id, {
            'q1_purpose': request.purpose
        })
        
        logger.info(f"✅ 목적 입력 완료: {request.session_id}")
        logger.info(f"   📝 목적: {request.purpose}")
        
        return PurposeResponse(
            message="목적이 설정되었습니다.",
            purpose=request.purpose
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 목적 입력 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"목적 입력 실패: {str(e)}")


@router.get("/warmup/{session_id}", response_model=WarmupResponse)
async def get_warmup_questions(
    session_id: str,
    session: dict = Depends(get_session_or_404)
):
    """
    Q2: LLM 기반 워밍업 질문 생성
    
    Args:
        session_id: 세션 ID
        session: 세션 데이터 (Dependency)
        
    Returns:
        WarmupResponse: 워밍업 질문 리스트 (2-3개)
    """
    try:
        purpose = session.get('q1_purpose')
        if not purpose:
            raise HTTPException(status_code=400, detail="Q1 목적이 입력되지 않았습니다.")
        
        logger.info(f"🤔 워밍업 질문 생성 시작: {session_id}")
        
        # LLM으로 워밍업 질문 생성
        prompt = f"""사용자가 "{purpose}"에 대한 아이디어를 생성하려고 합니다.

**목표**: 사용자의 직군/상황에 맞는 구체적인 워밍업 질문 2-3개 생성

**직군 추론**: 목적을 보고 사용자가 속한 직군(유튜버, 소상공인, 직장인, 학생, 개발자 등)을 파악하세요.

**워밍업 질문 생성 규칙**:
1. 사용자의 직군/상황에 맞는 **구체적인 질문**
2. 예: "누군가에게 자랑하고 싶은 결과물이라면 누구인가요?"
3. 2-3개의 질문만 생성
4. 각 질문은 간결하고 명확하게
5. 질문만 출력 (다른 설명 없이)

**출력 형식**:
- 질문1
- 질문2
- 질문3 (선택)
"""
        
        # Retry 로직으로 LLM 호출
        content = call_llm_with_retry(
            client=openai_client,
            model=llm_model,
            messages=[
                {"role": "system", "content": "당신은 유능한 기획자입니다."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.8,
            max_tokens=300
        )
        
        # 질문 파싱
        questions = [q.strip().lstrip('-').strip() for q in content.split('\n') if q.strip()]
        
        # 세션에 저장
        session_manager.update_session(session_id, {
            'q2_warmup_questions': questions
        })
        
        logger.info(f"✅ 워밍업 질문 생성 완료: {len(questions)}개")
        for i, q in enumerate(questions, 1):
            logger.info(f"   {i}. {q}")
        
        return WarmupResponse(questions=questions)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 워밍업 질문 생성 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"워밍업 질문 생성 실패: {str(e)}")


@router.post("/confirm/{session_id}", response_model=ConfirmResponse)
async def confirm_warmup(
    session_id: str,
    session: dict = Depends(get_session_or_404)
):
    """
    Q2: 워밍업 확인 (프론트엔드에서 "네" 버튼 클릭 시)
    
    Args:
        session_id: 세션 ID
        session: 세션 데이터 (Dependency)
        
    Returns:
        ConfirmResponse: 확인 메시지
    """
    logger.info(f"✅ 워밍업 확인 완료: {session_id}")
    return ConfirmResponse(message="워밍업이 확인되었습니다. Q3로 진행하세요.")


@router.post("/associations/{session_id}", response_model=AssociationsResponse)
async def submit_associations(
    session_id: str,
    request: AssociationsRequest,
    session: dict = Depends(get_session_or_404)
):
    """
    Q3: 자유연상 입력 (ChromaDB 기반 Ephemeral RAG)
    
    Args:
        session_id: 세션 ID
        request: 자유연상 키워드 리스트
        session: 세션 데이터 (Dependency)
        
    Returns:
        AssociationsResponse: 확인 메시지 및 입력 개수
    """
    try:
        logger.info(f"📝 자유연상 입력 시작: {session_id}")
        logger.info(f"   키워드: {request.associations}")
        
        # Ephemeral RAG 초기화 (JSON 기반)
        ephemeral_rag = EphemeralRAG(session_id=session_id)
        
        # 임베딩 및 JSON 저장
        ephemeral_rag.add_associations(request.associations)
        
        # 세션에 저장
        session_manager.update_session(session_id, {
            'q3_associations': request.associations,
            'ephemeral_rag_initialized': True
        })
        
        logger.info(f"✅ 자유연상 입력 완료: {len(request.associations)}개 키워드")
        logger.info(f"   📁 세션: {session_id}")
        
        return AssociationsResponse(
            message="자유연상 입력이 완료되었습니다.",
            count=len(request.associations)
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 자유연상 입력 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"자유연상 입력 실패: {str(e)}")


@router.get("/ideas/{session_id}", response_model=IdeaResponse)
async def generate_ideas(
    session_id: str,
    session: dict = Depends(get_session_or_404)
):
    """
    아이디어 생성 및 SWOT 분석
    
    Args:
        session_id: 세션 ID
        session: 세션 데이터 (Dependency)
        
    Returns:
        IdeaResponse: 아이디어 리스트
    """
    try:
        purpose = session.get('q1_purpose')
        associations = session.get('q3_associations', [])
        
        if not purpose or not associations:
            raise HTTPException(status_code=400, detail="Q1 또는 Q3 데이터가 없습니다.")
        
        logger.info(f"💡 아이디어 생성 시작: {session_id}")
        logger.info(f"   목적: {purpose}")
        logger.info(f"   키워드: {associations}")
        
        # Ephemeral RAG 초기화 (JSON 기반)
        ephemeral_rag = EphemeralRAG(session_id=session_id)
        
        # Q3 연상 키워드 추출 (유사도 기반)
        keywords_data = ephemeral_rag.extract_keywords_by_similarity(
            purpose=purpose,
            top_k=5
        )
        
        # 키워드만 추출
        extracted_keywords = [kw['keyword'] for kw in keywords_data]
        logger.info(f"   🔍 추출된 키워드: {extracted_keywords}")
        
        # 트렌드 키워드 검색 (네이버 뉴스, DuckDuckGo, 네이버 데이터랩)
        logger.info("   🌐 트렌드 키워드 검색 시작...")
        trend_keywords = await fetch_trend_keywords(purpose)
        
        # 트렌드 키워드 필터링 (사용자 키워드 기준)
        if trend_keywords:
            trend_keywords = ephemeral_rag.filter_trend_keywords(trend_keywords, top_k=10)
            logger.info(f"   🎯 필터링된 트렌드 키워드: {trend_keywords}")
        
        # 영구 RAG에서 브레인스토밍 기법 검색 (ChromaDB)
        rag_context = ""
        if permanent_collection:
            purpose_embedding = openai_client.embeddings.create(
                input=purpose,
                model=embedding_model
            ).data[0].embedding
            
            results = permanent_collection.query(
                query_embeddings=[purpose_embedding],
                n_results=3
            )
            
            if results and results.get('documents') and results['documents'][0]:
                # RAG 기법 포맷팅
                formatted_techniques = []
                for i, doc in enumerate(results['documents'][0], 1):
                    formatted_techniques.append(f"📌 **기법 {i}**:\n{doc}")
                rag_context = "\n\n---\n\n".join(formatted_techniques)
                logger.info(f"   📚 RAG 기법 {len(results['documents'][0])}개 검색됨")
        
        # 도메인 힌트 가져오기
        domain_hint = get_domain_hint(purpose)
        hint_text = format_hint_for_prompt(domain_hint) if domain_hint else ""
        
        # 아이디어 생성 프롬프트
        trend_str = ", ".join(trend_keywords) if trend_keywords else "없음"
        
        prompt = f"""**역할**: 당신은 창의적이면서도 현실적인 기획자입니다.

**목적**: "{purpose}"

**🔴 핵심: 사용자 브레인스토밍 키워드 (비중 80%)**
{', '.join(extracted_keywords)}
※ 위 키워드는 사용자가 직접 떠올린 것입니다. 이 키워드를 중심으로 아이디어를 구성하세요.

**🔵 참고: 최신 트렌드 키워드 (비중 20%)**
{trend_str}
※ 트렌드는 시의성 추가용으로만 살짝 활용하세요.

**브레인스토밍 기법 (필수 활용)**:

{rag_context}

💡 **기법 활용 방법**: 
- **각 아이디어마다 위의 기법 중 1-2개를 명시적으로 적용하세요**
- 예: "SCAMPER의 결합(Combine) 기법으로 A와 B를 합침" 
- 예: "마인드맵으로 중심 키워드에서 확장한 아이디어"

---

{hint_text}

**🚨 절대 규칙 (위반 시 답변 무효)**

1. **허구 데이터 절대 금지**
   ❌ 통계, 시장규모, 비용, 법규, 경쟁사 실적 등을 **절대 지어내지 마세요**
   ❌ "2023년 40억 명", "월 10만원", "연평균 9.1% 성장" 같은 **허구의 수치 금지**
   ✅ 모르면 언급하지 말고, 알고 있는 범위만 조심스럽게 표현하세요

2. **현실적 실행 가능성** (사용자 상황에 맞게 조절)
   ✅ 빠르게 시작 가능한 것 (며칠~몇 주 내)
   ✅ 초기 투자 부담이 크지 않은 범위
   ✅ 현재 가진 자원/역량으로 시도 가능한 것

3. **직군별 맞춤**
   - 유튜버 → 휴대폰 하나로 촬영 가능한 영상 구조
   - 소상공인 → 네이버/인스타로 당장 시작 가능한 홍보
   - 개발자 → 무료 API + 간단한 코드로 빠른 프로토타입
   - 학생 → 발표 자료, 구글 문서, PPT로 바로 작성
   - 회사원 → 팀 리소스 활용 가능한 실행 계획
   - 1인 사업자 → 최소 비용, 최대 효과

4. **보고서 스타일 금지, 행동 중심 작성**
   ❌ "효율적인 마케팅 전략 수립을 통해..." (거창한 전략)
   ✅ "네이버 블로그 만들고, 첫 글 3개 올린다. 제목에 '지역명+업종' 넣는다." (구체적 행동)

---

**핵심 요구사항**:

1. **직군 파악**: 목적을 보고 사용자의 직군/상황을 정확히 파악하세요

2. **문제 중심 접근**:
   - 💡 핵심 문제: 사용자가 **실제로 겪고 있는 구체적 불편함**을 먼저 정의
   - 예: "소상공인은 쿠폰을 수기로 관리하다 단골 이탈률이 높음"

3. **브레인스토밍 기법으로 아이디어 발상**:
   - **위 RAG 기법을 반드시 1개 이상 명시적으로 사용**

4. **개선 방안 (기대 효과)**:
   - 이 아이디어가 문제를 **어떻게** 해결하는지
   - **구체적인 효과**를 제시

5. **분석 결과** (각 항목 1-2줄, 간결하게):
   - 강점: 이 아이디어만의 차별점
   - 약점: 현실적인 리스크
   - 기회: 시장 트렌드와의 연결
   - 위협: 경쟁 상황

**금지 사항**:
❌ 마크다운 볼드체(**) 사용 금지, 이모지와 일반 텍스트만

**출력 형식**:

아이디어 1: [해결책을 함축한 구체적 제목]

📌 상황과 문제
[누가/어떤 사람이] [어떤 상황에서] [무엇 때문에] 불편함을 겪고 있습니다.
구체적으로 설명하면, [문제의 핵심 원인]으로 인해 [어떤 결과/손해]가 발생합니다.
(3-4줄로 상황을 생생하게 묘사)

💡 해결 아이디어: [위 제목을 다시 언급]
이 문제를 해결하기 위해 [제목의 핵심 개념]을 제안합니다.
[구체적으로 어떻게 작동하는지], [사용자가 어떤 행동을 하면 어떤 결과가 나오는지] 설명합니다.
(3-4줄로 솔루션의 작동 방식을 자연스럽게 연결)

🎯 기대 효과
이 아이디어를 적용하면:
- [첫 번째 구체적인 변화/개선점]
- [두 번째 구체적인 변화/개선점]
(각 효과는 위 문제와 직접 연결되어야 함)

🎨 발상 기법
[사용한 브레인스토밍 기법명]을 활용했습니다.
[그 기법을 어떻게 적용해서 이 아이디어가 나왔는지 한 줄로 설명]

📊 분석 결과
• 강점: [2개, 각 1줄]
• 약점: [2개, 각 1줄]  
• 기회: [2개, 각 1줄]
• 위협: [2개, 각 1줄]

---

아이디어 2: [해결책을 함축한 구체적 제목]
(동일한 형식)

---

아이디어 3: [해결책을 함축한 구체적 제목] (선택)
(동일한 형식)

**중요**: 
- 제목은 반드시 "해결책"을 함축해야 합니다 (예: "유튜브 클립 자동저장 북마크")
- "상황과 문제"에서 제시한 문제가 "해결 아이디어"에서 직접 해결되어야 합니다
- 각 섹션이 논리적으로 연결되어 하나의 스토리처럼 읽혀야 합니다

**반드시 2-3개의 완전한 아이디어를 생성해야 합니다.**
"""
        
        logger.info("   🤖 LLM 아이디어 생성 중...")
        
        # Retry 로직으로 LLM 호출
        ideas_text = call_llm_with_retry(
            client=openai_client,
            model=llm_model,
            messages=[
                {"role": "system", "content": "당신은 현실적인 기획자입니다. 허구의 통계나 비용을 절대 지어내지 않으며, 사용자가 가진 자원과 역량으로 빠르게 시작 가능한 아이디어를 제안합니다. **반드시 2-3개의 완전한 아이디어를 생성해야 합니다.**"},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=2000
        )
        
        # 🔥 아이디어 파싱
        ideas = []
        current_idea = None
        current_section = None
        
        import re
        
        for line in ideas_text.split('\n'):
            line = line.strip()
            if not line or line == '---':
                continue
            
            # 아이디어 시작
            if re.match(r'^아이디어\s+\d+:', line):
                if current_idea:
                    ideas.append(current_idea)
                
                title = line.split(':', 1)[1].strip() if ':' in line else line
                current_idea = {
                    'title': title,
                    'description': '',
                    'analysis': ''
                }
                current_section = None
            
            # 섹션 구분
            elif current_idea:
                if '📌 상황과 문제' in line or '상황과 문제' in line:
                    current_section = 'problem'
                    current_idea['description'] += '\n📌 상황과 문제\n'
                elif '💡 해결 아이디어' in line or '해결 아이디어' in line:
                    current_section = 'solution'
                    current_idea['description'] += '\n\n💡 해결 아이디어\n'
                elif '🎯 기대 효과' in line or '기대 효과' in line:
                    current_section = 'effect'
                    current_idea['description'] += '\n\n🎯 기대 효과\n'
                elif '🎨 발상 기법' in line or '발상 기법' in line:
                    current_section = 'technique'
                    current_idea['description'] += '\n\n🎨 발상 기법\n'
                elif '📊 분석 결과' in line or '분석 결과:' in line or '📊 SWOT 분석' in line:
                    current_section = 'analysis'
                    current_idea['description'] += '\n\n📊 분석 결과\n'
                
                # 내용 추가
                elif current_section in ['problem', 'solution', 'effect', 'technique']:
                    current_idea['description'] += line + '\n'
                elif current_section == 'analysis':
                    current_idea['description'] += line + '\n'
        
        if current_idea:
            ideas.append(current_idea)
        
        # 아이디어 검증
        if not ideas:
            logger.error("❌ 아이디어 파싱 실패")
            raise HTTPException(
                status_code=500,
                detail="아이디어 생성에 실패했습니다."
            )
        
        # description과 analysis 분리
        for idea in ideas:
            full_text = idea['description']
            
            if '📊 분석 결과:' in full_text:
                parts = full_text.split('📊 분석 결과:')
                idea['description'] = parts[0].strip()
                idea['analysis'] = '📊 분석 결과:\n' + parts[1].strip()
            elif '📊 SWOT 분석:' in full_text:
                parts = full_text.split('📊 SWOT 분석:')
                idea['description'] = parts[0].strip()
                idea['analysis'] = '📊 분석 결과:\n' + parts[1].strip()
            else:
                idea['analysis'] = ''
        
        logger.info(f"✅ 아이디어 생성 완료: {len(ideas)}개")
        for i, idea in enumerate(ideas, 1):
            logger.info(f"   {i}. {idea['title']}")
        
        # 세션에 저장
        session_manager.update_session(session_id, {
            'generated_ideas': ideas
        })
        
        return IdeaResponse(ideas=ideas)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 아이디어 생성 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"아이디어 생성 실패: {str(e)}")


@router.delete("/session/{session_id}", response_model=DeleteResponse)
async def delete_session(
    session_id: str,
    session: dict = Depends(get_session_or_404)
):
    """
    세션 삭제 (임시 데이터 모두 삭제)
    
    Args:
        session_id: 세션 ID
        session: 세션 데이터 (Dependency)
        
    Returns:
        DeleteResponse: 확인 메시지
    """
    try:
        logger.info(f"🗑️  세션 삭제 시작: {session_id}")
        
        # Ephemeral RAG 데이터 삭제 (JSON 기반)
        ephemeral_rag = EphemeralRAG(session_id=session_id)
        deleted = ephemeral_rag.delete_session_data()
        
        if deleted:
            logger.info(f"   ✅ 세션 데이터 삭제: {session_id}")
        else:
            logger.info("   ℹ️  삭제할 세션 데이터 없음")
        
        # 세션 디렉토리 삭제
        ephemeral_dir = Path(session['directory'])
        if ephemeral_dir.exists():
            shutil.rmtree(ephemeral_dir)
            logger.info(f"   ✅ 디렉토리 삭제: {ephemeral_dir}")
        
        # 세션 매니저에서 삭제
        session_manager.delete_session(session_id)
        logger.info(f"   ✅ 세션 매니저에서 삭제 완료")
        
        logger.info(f"✅ 세션 삭제 완료: {session_id}")
        
        return DeleteResponse(message="세션이 삭제되었습니다.")
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 세션 삭제 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"세션 삭제 실패: {str(e)}")
