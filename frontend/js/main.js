// ========================================
// 메인 페이지 JavaScript
// ========================================

// 사이드바 토글 함수
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('active');
}

// 브레인스토밍 시작 함수
function startBrainstorming() {
    // 브레인스토밍 페이지로 이동
    window.location.href = 'brainstorm.html';
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('메인 페이지 로드 완료');
    
    // 사이드바 외부 클릭 시 닫기
    document.addEventListener('click', function(e) {
        const sidebar = document.getElementById('sidebar');
        const navMenu = document.querySelector('.nav-menu');
        
        // 사이드바가 열려있고, 클릭이 사이드바 외부이고, 네비게이션 메뉴도 아닐 때
        if (sidebar.classList.contains('active') && 
            !sidebar.contains(e.target) && 
            !navMenu.contains(e.target)) {
            sidebar.classList.remove('active');
        }
    });
    
    // 아이디어 항목 클릭 이벤트 (임시)
    const ideaItems = document.querySelectorAll('.idea-item');
    ideaItems.forEach(item => {
        item.addEventListener('click', function() {
            const title = this.querySelector('.idea-title').textContent;
            alert(`"${title}" 상세보기 기능은 준비 중입니다.`);
        });
    });
});
