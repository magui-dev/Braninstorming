# 💡 아이디어 생성기 - AI 브레인스토밍 플랫폼

AI와 함께 창의적인 아이디어를 만들어보세요!

## 🎯 주요 기능

- **AI 브레인스토밍**: 5단계 워크플로우로 아이디어 생성
- **소셜 로그인**: Google, Kakao, Naver OAuth 2.0
- **비로그인 저장**: 로그인 없이 아이디어 임시 저장 → 로그인 후 자동 연결
- **트렌드 반영**: 네이버 뉴스, 데이터랩, DuckDuckGo 트렌드 검색 통합
- **RAG 기반**: SCAMPER, Mind Mapping 등 브레인스토밍 기법 활용

## 🏗️ 기술 스택

| 영역 | 기술 |
|------|------|
| **백엔드** | Spring Boot 3.5.7, Java 17, JPA, MySQL |
| **AI 서비스** | Python FastAPI, OpenAI GPT-4o, ChromaDB |
| **프론트엔드** | Vanilla JS, HTML/CSS |
| **인증** | OAuth 2.0 + JWT |
| **배포** | Docker, Nginx |

## 📁 프로젝트 구조

```
brainstorming-platform/
├── src/                    # Spring Boot 백엔드
│   └── main/java/com/brainstorming/
│       └── domain/
│           ├── user/       # 사용자 관리
│           ├── idea/       # 아이디어 CRUD
│           ├── inquiry/    # 문의 관리
│           └── brainstorming/  # Python 연동
├── python-service/         # FastAPI AI 서비스
│   └── app/domain/brainstorming/
│       ├── search/         # 트렌드 검색 (네이버, DuckDuckGo)
│       ├── data/chroma/    # 영구 RAG (브레인스토밍 기법)
│       └── data/ephemeral/ # 임시 RAG (세션별)
├── frontend/               # 프론트엔드
│   ├── index.html
│   ├── brainstorm.html
│   └── js/
└── docker-compose.yml
```

## 🔄 브레인스토밍 플로우

```
1. 목적 입력 → 2. 워밍업 질문 → 3. 자유연상 (10개+)
                                        ↓
4. AI 키워드 분석 ← 트렌드 검색 (사용자 80% + 트렌드 20%)
                                        ↓
5. 아이디어 생성 (3개) ← RAG 기법 + GPT-4o
```

## 🚀 실행 방법

### 1. 환경 변수 설정

```bash
# .env (루트)
MYSQL_ROOT_PASSWORD=your_password
JWT_SECRET=your-256-bit-secret-key

# python-service/.env
OPENAI_API_KEY=sk-xxx
NAVER_SEARCH_CLIENT_ID=xxx      # 선택
NAVER_SEARCH_CLIENT_SECRET=xxx  # 선택
```

### 2. 로컬 실행

```bash
# 백엔드 (터미널 1)
./gradlew bootRun

# Python 서비스 (터미널 2)
cd python-service
pip install -r requirements.txt
python main.py

# 프론트엔드 (터미널 3)
cd frontend
# Live Server 또는 직접 열기
```

### 3. Docker 실행

```bash
docker-compose up -d
```

## 🔑 API 엔드포인트

### Spring Boot (:8080)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/auth/me | 현재 사용자 정보 |
| GET | /api/ideas?userId={id} | 아이디어 목록 |
| POST | /api/ideas | 아이디어 저장 |
| POST | /api/ideas/link-guest | 게스트 아이디어 연결 |

### Python FastAPI (:8000)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/brainstorming/session | 세션 생성 |
| POST | /api/v1/brainstorming/purpose | 목적 입력 |
| GET | /api/v1/brainstorming/warmup/{id} | 워밍업 질문 |
| POST | /api/v1/brainstorming/associations/{id} | 자유연상 입력 |
| GET | /api/v1/brainstorming/ideas/{id} | 아이디어 생성 |

## 📝 주요 설정

### application.yaml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brainstorm
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: ${JWT_SECRET}
  expiration: 7200000  # 2시간
```

## 🧹 자동 정리

- **게스트 아이디어**: 매일 새벽 3시, 1일 지난 미연결 데이터 삭제
- **Python 세션**: 24시간 지난 ephemeral 폴더 자동 정리

## 📄 라이선스

MIT License

---

Made with  by jinmo
