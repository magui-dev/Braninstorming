// ========================================
// 메인 페이지 JavaScript (OAuth + Inquiry + Idea Management)
// ========================================

let currentUser = null; // 현재 로그인한 사용자

// ========================================
// 페이지 로드 시 실행
// ========================================
document.addEventListener('DOMContentLoaded', async function() {
    console.log('메인 페이지 로드 완료');
    
    // 로그인 상태 확인
    await checkLoginStatus();
    
    // 사이드바 외부 클릭 시 닫기
    document.addEventListener('click', function(e) {
        const sidebar = document.getElementById('sidebar');
        const navMenu = document.querySelector('.nav-menu');
        
        if (sidebar.classList.contains('active') && 
            !sidebar.contains(e.target) && 
            !navMenu.contains(e.target)) {
            sidebar.classList.remove('active');
        }
    });
});

// ========================================
// 로그인 상태 확인
// ========================================
async function checkLoginStatus() {
    const token = localStorage.getItem('token');
    
    if (!token) {
        showLoginButton();
        return;
    }
    
    try {
        // 토큰 유효성 검증
        const response = await fetch(`${CONFIG.SPRING_API_BASE}/auth/me`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('인증 실패');
        }
        
        currentUser = await response.json();
        console.log('✅ 로그인 확인:', currentUser);
        
        // 🆕 게스트 아이디어 연결 처리
        await linkPendingGuestIdeas(currentUser.userId);
        
        showUserInfo(currentUser);
        await loadMyIdeas(currentUser.userId);
        
    } catch (error) {
        console.error('❌ 로그인 확인 실패:', error);
        localStorage.removeItem('token');
        showLoginButton();
    }
}

// ========================================
// 🆕 게스트 아이디어 연결 (로그인 후 처리)
// ========================================
async function linkPendingGuestIdeas(userId) {
    const pendingGuestSessionId = localStorage.getItem('pendingGuestSessionId');
    
    if (!pendingGuestSessionId) {
        return; // 연결할 게스트 아이디어 없음
    }
    
    try {
        console.log('🔗 게스트 아이디어 연결 시도:', pendingGuestSessionId);
        
        const response = await fetch(
            `${CONFIG.SPRING_API_BASE}/ideas/link-guest?guestSessionId=${encodeURIComponent(pendingGuestSessionId)}&userId=${userId}`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                }
            }
        );
        
        if (!response.ok) {
            throw new Error('게스트 아이디어 연결 실패');
        }
        
        const linkedCount = await response.json();
        console.log('✅ 게스트 아이디어 연결 완료:', linkedCount, '개');
        
        // localStorage에서 제거
        localStorage.removeItem('pendingGuestSessionId');
        
        if (linkedCount > 0) {
            alert(`✅ 임시 저장된 아이디어 ${linkedCount}개가 계정에 연결되었습니다!\n\n"나의 아이디어"에서 확인하세요.`);
        }
        
    } catch (error) {
        console.error('❌ 게스트 아이디어 연결 오류:', error);
        // 실패해도 localStorage에서 제거 (무한 루프 방지)
        localStorage.removeItem('pendingGuestSessionId');
    }
}

// ========================================
// 사용자 정보 표시
// ========================================
function showUserInfo(user) {
    document.getElementById('loginBtn').style.display = 'none';
    document.getElementById('userInfo').style.display = 'block';
    document.getElementById('logoutBtn').style.display = 'block';
    document.getElementById('userName').textContent = `${user.username}님`;
}

// ========================================
// 로그인 버튼 표시
// ========================================
function showLoginButton() {
    document.getElementById('loginBtn').style.display = 'block';
    document.getElementById('userInfo').style.display = 'none';
    document.getElementById('logoutBtn').style.display = 'none';
}

// ========================================
// 로그인 모달 열기
// ========================================
function showLoginModal() {
    document.getElementById('loginModal').style.display = 'flex';
}

// ========================================
// 로그인 모달 닫기
// ========================================
function closeLoginModal() {
    document.getElementById('loginModal').style.display = 'none';
}

// ========================================
// 로그아웃
// ========================================
function logout() {
    localStorage.removeItem('token');
    currentUser = null;
    location.reload();
}

// ========================================
// 문의하기 모달 열기
// ========================================
function showInquiryModal() {
    if (!currentUser) {
        alert('로그인이 필요합니다.');
        showLoginModal();
        return;
    }
    document.getElementById('inquiryModal').style.display = 'flex';
}

// ========================================
// 문의하기 모달 닫기
// ========================================
function closeInquiryModal() {
    document.getElementById('inquiryModal').style.display = 'none';
    document.getElementById('inquiryTitle').value = '';
    document.getElementById('inquiryContent').value = '';
}

// ========================================
// 문의하기 제출
// ========================================
async function submitInquiry() {
    const title = document.getElementById('inquiryTitle').value.trim();
    const content = document.getElementById('inquiryContent').value.trim();
    
    if (!title || !content) {
        alert('제목과 내용을 모두 입력해주세요.');
        return;
    }
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${CONFIG.SPRING_API_BASE}/inquiries`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId: currentUser.userId,
                title: title,
                content: content
            })
        });
        
        if (!response.ok) {
            throw new Error('문의 제출 실패');
        }
        
        alert('✅ 문의가 접수되었습니다!\n빠른 시일 내에 답변드리겠습니다.');
        closeInquiryModal();
        
    } catch (error) {
        console.error('❌ 문의 제출 오류:', error);
        alert('❌ 문의 제출에 실패했습니다.');
    }
}

// ========================================
// 나의 아이디어 불러오기
// ========================================
async function loadMyIdeas(userId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${CONFIG.SPRING_API_BASE}/ideas?userId=${userId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('아이디어 목록 로드 실패');
        }
        
        const ideas = await response.json();
        console.log('✅ 아이디어 목록:', ideas);
        
        displayIdeas(ideas);
        
    } catch (error) {
        console.error('❌ 아이디어 목록 로드 실패:', error);
    }
}

// ========================================
// 아이디어 표시
// ========================================
function displayIdeas(ideas) {
    const sidebarContent = document.querySelector('.sidebar-content');
    sidebarContent.innerHTML = '';
    
    if (ideas.length === 0) {
        sidebarContent.innerHTML = '<p class="empty-message">저장된 아이디어가 없습니다.</p>';
        return;
    }
    
    ideas.forEach(idea => {
        const ideaItem = document.createElement('div');
        ideaItem.className = 'idea-item';
        ideaItem.style.cursor = 'pointer';
        ideaItem.innerHTML = `
            <div class="idea-date">${formatDate(idea.createdAt)}</div>
            <div class="idea-title">${idea.title}</div>
            <button class="delete-idea-btn" onclick="deleteIdea(${idea.ideaId}, event)">🗑️</button>
        `;
        
        // 아이디어 클릭 이벤트
        ideaItem.addEventListener('click', () => showIdeaDetail(idea.ideaId));
        
        sidebarContent.appendChild(ideaItem);
    });
}

// ========================================
// 날짜 포맷팅
// ========================================
function formatDate(dateString) {
    const date = new Date(dateString);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
}

// ========================================
// 아이디어 삭제
// ========================================
async function deleteIdea(ideaId, event) {
    event.stopPropagation(); // 부모 요소 클릭 이벤트 차단
    
    if (!confirm('정말 이 아이디어를 삭제하시겠습니까?')) {
        return;
    }
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${CONFIG.SPRING_API_BASE}/ideas/${ideaId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('아이디어 삭제 실패');
        }
        
        console.log('✅ 아이디어 삭제 완료:', ideaId);
        
        // 목록 새로고침
        await loadMyIdeas(currentUser.userId);
        
    } catch (error) {
        console.error('❌ 아이디어 삭제 오류:', error);
        alert('❌ 아이디어 삭제에 실패했습니다.');
    }
}

// ========================================
// 사이드바 토글
// ========================================
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('active');
}

// ========================================
// 브레인스토밍 시작
// ========================================
function startBrainstorming() {
    window.location.href = 'brainstorm.html';
}

// ========================================
// 아이디어 상세보기
// ========================================
async function showIdeaDetail(ideaId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${CONFIG.SPRING_API_BASE}/ideas/${ideaId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('아이디어 상세 조회 실패');
        }
        
        const idea = await response.json();
        console.log('✅ 아이디어 상세:', idea);
        
        // 모달 표시
        displayIdeaModal(idea);
        
    } catch (error) {
        console.error('❌ 아이디어 상세 조회 오류:', error);
        alert('❌ 아이디어를 불러오는데 실패했습니다.');
    }
}

// ========================================
// 아이디어 상세 모달 표시
// ========================================
function displayIdeaModal(idea) {
    // content는 JSON 문자열이므로 파싱
    let ideaContent;
    try {
        ideaContent = JSON.parse(idea.content);
    } catch (e) {
        console.error('JSON 파싱 실패:', e);
        ideaContent = { description: idea.content, analysis: '' };
    }
    
    const modal = document.createElement('div');
    modal.id = 'ideaDetailModal';
    modal.style.cssText = 'display: flex; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 10000; align-items: center; justify-content: center; padding: 2rem;';
    
    modal.innerHTML = `
        <div style="background: white; border-radius: 16px; width: 90%; max-width: 800px; max-height: 85vh; overflow-y: auto; padding: 2.5rem; position: relative;">
            <button onclick="closeIdeaModal()" style="position: absolute; top: 1.5rem; right: 1.5rem; background: none; border: none; font-size: 2rem; cursor: pointer; color: #7f8c8d;">×</button>
            
            <h2 style="font-size: 2rem; color: #2c3e50; margin-bottom: 1.5rem; padding-right: 2rem;">
                💡 ${idea.title}
            </h2>
            
            <div style="color: #7f8c8d; font-size: 0.95rem; margin-bottom: 2rem;">
                생성일: ${formatDate(idea.createdAt)}
            </div>
            
            <div style="background: #f8f9fa; padding: 2rem; border-radius: 12px; margin-bottom: 2rem;">
                <h3 style="font-size: 1.3rem; color: #2c3e50; margin-bottom: 1rem;">📝 아이디어 설명</h3>
                <div style="line-height: 1.8; color: #2c3e50; white-space: pre-wrap;">
                    ${ideaContent.description || idea.content}
                </div>
            </div>
            
            ${ideaContent.analysis ? `
                <div style="background: #e8f5e9; padding: 2rem; border-radius: 12px; border-left: 4px solid #4caf50;">
                    <h3 style="font-size: 1.3rem; color: #2c3e50; margin-bottom: 1rem;">📊 분석 결과</h3>
                    <div style="line-height: 1.8; color: #2c3e50; white-space: pre-wrap;">
                        ${ideaContent.analysis}
                    </div>
                </div>
            ` : ''}
            
            <div style="text-align: center; margin-top: 2rem;">
                <button onclick="closeIdeaModal()" style="padding: 1rem 3rem; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 8px; font-size: 1.1rem; font-weight: 600; cursor: pointer;">
                    닫기
                </button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 모달 외부 클릭 시 닫기
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeIdeaModal();
        }
    });
}

// ========================================
// 아이디어 모달 닫기
// ========================================
function closeIdeaModal() {
    const modal = document.getElementById('ideaDetailModal');
    if (modal) {
        modal.remove();
    }
}
