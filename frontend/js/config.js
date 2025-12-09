// ========================================
// 환경별 설정 (로컬 개발 / 프로덕션 자동 전환)
// ========================================

const ENV = {
    // 로컬 개발 환경
    development: {
        SPRING_API_BASE: 'http://localhost:8080/api',
        PYTHON_API_BASE: 'http://localhost:8000/api/v1/brainstorming'
    },
    // 프로덕션 (Docker/Nginx)
    production: {
        SPRING_API_BASE: '/api',
        PYTHON_API_BASE: '/api/v1/brainstorming'
    }
};

// ========================================
// 환경 자동 감지
// ========================================
const isLocalhost = 
    window.location.hostname === 'localhost' || 
    window.location.hostname === '127.0.0.1' ||
    window.location.protocol === 'file:';

// 수동으로 환경 강제 설정하려면 아래 주석 해제
// const FORCE_ENV = 'development';  // 또는 'production'

const currentEnv = typeof FORCE_ENV !== 'undefined' ? FORCE_ENV : (isLocalhost ? 'development' : 'production');

// ========================================
// 현재 환경 설정 내보내기
// ========================================
const CONFIG = {
    ...ENV[currentEnv],
    ENV_NAME: currentEnv,
    IS_LOCAL: isLocalhost
};

// 콘솔에 현재 환경 출력 (디버깅용)
console.log(`🔧 Environment: ${CONFIG.ENV_NAME}`);
console.log(`   - Spring API: ${CONFIG.SPRING_API_BASE}`);
console.log(`   - Python API: ${CONFIG.PYTHON_API_BASE}`);
