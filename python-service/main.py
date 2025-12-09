from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import os

# 환경변수 로드
load_dotenv()

# FastAPI 앱 생성
app = FastAPI(
    title="Brainstorming API",
    version="1.0.0",
    description="AI-powered Brainstorming Module for Java Integration"
)

# CORS 설정 (환경변수로 동적 설정)
# 기본값: 로컬 개발용
allowed_origins = os.getenv(
    "ALLOWED_ORIGINS",
    "http://localhost:8080,http://localhost:3000"
).split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 브레인스토밍 라우터 등록
from app.api.v1.endpoints import brainstorming
app.include_router(
    brainstorming.router, 
    prefix="/api/v1/brainstorming",
    tags=["brainstorming"]
)

@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": "Brainstorming API",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/favicon.ico")
async def favicon():
    """favicon 요청 무시 (204 No Content)"""
    from fastapi.responses import Response
    return Response(status_code=204)

@app.get("/health")
async def health_check():
    """헬스 체크"""
    return {
        "status": "healthy",
        "service": "Brainstorming API",
        "openai_key_set": bool(os.getenv("OPENAI_API_KEY"))
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )
