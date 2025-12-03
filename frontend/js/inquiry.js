// ========================================
// 문의하기 페이지 JavaScript
// ========================================

let currentUser = null;
let inquiries = [];
let selectedInquiry = null;

// ========================================
// 페이지 로드
// ========================================
document.addEventListener('DOMContentLoaded', async function() {
    console.log('문의 페이지 로드');
    
    // 로그인 확인
    await checkLoginStatus();
    
    // 문의 목록 로드
    if (currentUser) {
        await loadInquiries();
    }
    
    // 폼 제출 이벤트
    document.getElementById('inquiryForm').addEventListener('submit', handleSubmit);
});

// ========================================
// 로그인 확인
// ========================================
async function checkLoginStatus() {
    const token = localStorage.getItem('token');
    
    if (!token) {
        alert('로그인이 필요합니다.');
        location.href = 'index.html';
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8080/api/auth/me', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('인증 실패');
        }
        
        currentUser = await response.json();
        document.getElementById('userName').textContent = `${currentUser.username}님`;
        
    } catch (error) {
        console.error('❌ 로그인 확인 실패:', error);
        alert('로그인이 만료되었습니다.');
        localStorage.removeItem('token');
        location.href = 'index.html';
    }
}

// ========================================
// 로그아웃
// ========================================
function logout() {
    localStorage.removeItem('token');
    location.href = 'index.html';
}

// ========================================
// 탭 전환
// ========================================
function switchTab(tab) {
    // 탭 버튼 활성화
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    
    // 탭 내용 표시
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    
    if (tab === 'list') {
        document.getElementById('listTab').classList.add('active');
        loadInquiries();
    } else {
        document.getElementById('writeTab').classList.add('active');
        // 폼 초기화
        document.getElementById('inquiryForm').reset();
    }
}

// ========================================
// 문의 목록 로드
// ========================================
async function loadInquiries() {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`http://localhost:8080/api/inquiries?userId=${currentUser.userId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('문의 목록 로드 실패');
        }
        
        inquiries = await response.json();
        console.log('✅ 문의 목록:', inquiries);
        
        displayInquiries();
        
    } catch (error) {
        console.error('❌ 문의 목록 로드 실패:', error);
        alert('문의 목록을 불러오는데 실패했습니다.');
    }
}

// ========================================
// 문의 목록 표시
// ========================================
function displayInquiries() {
    const listContainer = document.getElementById('inquiryList');
    
    if (inquiries.length === 0) {
        listContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📭</div>
                <h3>등록된 문의가 없습니다</h3>
                <p>궁금한 점이나 건의사항을 문의해주세요!</p>
            </div>
        `;
        return;
    }
    
    // 최신순 정렬
    inquiries.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    
    listContainer.innerHTML = inquiries.map(inquiry => `
        <div class="inquiry-card" onclick="showDetailModal(${inquiry.inquiryId})">
            <div class="inquiry-card-header">
                <span class="inquiry-status ${inquiry.status.toLowerCase()}">
                    ${getStatusText(inquiry.status)}
                </span>
                <span class="inquiry-date">${formatDate(inquiry.createdAt)}</span>
            </div>
            <div class="inquiry-title">${inquiry.title}</div>
        </div>
    `).join('');
}

// ========================================
// 상태 텍스트 변환
// ========================================
function getStatusText(status) {
    const statusMap = {
        'PENDING': '답변 대기',
        'ANSWERED': '답변 완료',
        'CLOSED': '종료'
    };
    return statusMap[status] || status;
}

// ========================================
// 날짜 포맷
// ========================================
function formatDate(dateString) {
    const date = new Date(dateString);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
}

// ========================================
// 문의 제출
// ========================================
async function handleSubmit(e) {
    e.preventDefault();
    
    const title = document.getElementById('inquiryTitle').value.trim();
    const content = document.getElementById('inquiryContent').value.trim();
    
    if (!title || !content) {
        alert('제목과 내용을 모두 입력해주세요.');
        return;
    }
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch('http://localhost:8080/api/inquiries', {
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
        
        alert('✅ 문의가 성공적으로 제출되었습니다!');
        
        // 폼 초기화
        document.getElementById('inquiryForm').reset();
        
        // 목록 탭으로 이동
        document.querySelectorAll('.tab-btn')[0].click();
        
    } catch (error) {
        console.error('❌ 문의 제출 오류:', error);
        alert('❌ 문의 제출에 실패했습니다.');
    }
}

// ========================================
// 문의 상세 모달
// ========================================
async function showDetailModal(inquiryId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`http://localhost:8080/api/inquiries/${inquiryId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('문의 상세 로드 실패');
        }
        
        selectedInquiry = await response.json();
        
        // 모달 내용 생성
        const modalContent = document.getElementById('modalContent');
        const modalActions = document.getElementById('modalActions');
        
        modalContent.innerHTML = `
            <div class="inquiry-detail-section">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <h3>${selectedInquiry.title}</h3>
                    <span class="inquiry-status ${selectedInquiry.status.toLowerCase()}">
                        ${getStatusText(selectedInquiry.status)}
                    </span>
                </div>
                <div style="color: #7f8c8d; margin-bottom: 1rem;">
                    작성일: ${formatDate(selectedInquiry.createdAt)}
                </div>
            </div>
            
            <div class="inquiry-detail-section">
                <h3>문의 내용</h3>
                <div class="inquiry-detail-text">
                    ${selectedInquiry.content.replace(/\n/g, '<br>')}
                </div>
            </div>
            
            ${selectedInquiry.reply ? `
                <div class="inquiry-detail-section">
                    <h3>✅ 답변</h3>
                    <div class="inquiry-reply">
                        ${selectedInquiry.reply.replace(/\n/g, '<br>')}
                    </div>
                </div>
            ` : `
                <div class="inquiry-detail-section">
                    <div style="text-align: center; padding: 2rem; background: #f8f9fa; border-radius: 8px; color: #7f8c8d;">
                        ⏳ 답변 대기 중입니다
                    </div>
                </div>
            `}
        `;
        
        // 액션 버튼
        modalActions.innerHTML = '';
        
        // PENDING 상태일 때만 수정/삭제 가능
        if (selectedInquiry.status === 'PENDING') {
            modalActions.innerHTML = `
                <button class="btn btn-secondary" onclick="editInquiry(${inquiryId})">수정하기</button>
                <button class="btn btn-danger" onclick="deleteInquiry(${inquiryId})">삭제하기</button>
                <button class="btn btn-primary" onclick="closeDetailModal()">닫기</button>
            `;
        } else {
            modalActions.innerHTML = `
                <button class="btn btn-primary" onclick="closeDetailModal()">닫기</button>
            `;
        }
        
        // 모달 표시
        document.getElementById('inquiryDetailModal').style.display = 'flex';
        
    } catch (error) {
        console.error('❌ 문의 상세 로드 실패:', error);
        alert('문의 내용을 불러오는데 실패했습니다.');
    }
}

// ========================================
// 모달 닫기
// ========================================
function closeDetailModal() {
    document.getElementById('inquiryDetailModal').style.display = 'none';
    selectedInquiry = null;
}

// ========================================
// 문의 수정
// ========================================
async function editInquiry(inquiryId) {
    const title = prompt('새 제목:', selectedInquiry.title);
    if (!title) return;
    
    const content = prompt('새 내용:', selectedInquiry.content);
    if (!content) return;
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`http://localhost:8080/api/inquiries/${inquiryId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ title, content })
        });
        
        if (!response.ok) {
            throw new Error('문의 수정 실패');
        }
        
        alert('✅ 문의가 수정되었습니다!');
        closeDetailModal();
        await loadInquiries();
        
    } catch (error) {
        console.error('❌ 문의 수정 오류:', error);
        alert('❌ 문의 수정에 실패했습니다.');
    }
}

// ========================================
// 문의 삭제
// ========================================
async function deleteInquiry(inquiryId) {
    if (!confirm('정말 이 문의를 삭제하시겠습니까?')) {
        return;
    }
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`http://localhost:8080/api/inquiries/${inquiryId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('문의 삭제 실패');
        }
        
        alert('✅ 문의가 삭제되었습니다!');
        closeDetailModal();
        await loadInquiries();
        
    } catch (error) {
        console.error('❌ 문의 삭제 오류:', error);
        alert('❌ 문의 삭제에 실패했습니다.');
    }
}
