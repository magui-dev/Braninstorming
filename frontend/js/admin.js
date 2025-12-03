// ========================================
// 관리자 페이지 JavaScript
// ========================================

let currentUser = null;
let allInquiries = [];
let filteredInquiries = [];
let currentFilter = 'ALL';
let selectedInquiry = null;

// ========================================
// 페이지 로드
// ========================================
document.addEventListener('DOMContentLoaded', async function() {
    console.log('관리자 페이지 로드');
    
    // 로그인 확인 (관리자 권한 체크)
    await checkAdminAuth();
    
    // 문의 목록 로드
    await loadAllInquiries();
});

// ========================================
// 관리자 권한 확인
// ========================================
async function checkAdminAuth() {
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
        
        // ADMIN 권한 체크
        if (currentUser.role !== 'ADMIN') {
            alert('⚠️ 관리자 권한이 필요합니다.');
            location.href = 'index.html';
            return;
        }
        
        document.getElementById('adminName').textContent = `${currentUser.username}님`;
        
    } catch (error) {
        console.error('❌ 인증 실패:', error);
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
// 전체 문의 목록 로드
// ========================================
async function loadAllInquiries() {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch('http://localhost:8080/api/inquiries/admin/all', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('문의 목록 로드 실패');
        }
        
        allInquiries = await response.json();
        console.log('✅ 전체 문의:', allInquiries.length + '개');
        
        updateStats();
        filterInquiries(currentFilter);
        
    } catch (error) {
        console.error('❌ 문의 목록 로드 실패:', error);
        alert('문의 목록을 불러오는데 실패했습니다.');
    }
}

// ========================================
// 통계 업데이트
// ========================================
function updateStats() {
    const pending = allInquiries.filter(i => i.status === 'PENDING').length;
    const answered = allInquiries.filter(i => i.status === 'ANSWERED').length;
    const closed = allInquiries.filter(i => i.status === 'CLOSED').length;
    
    document.getElementById('statPending').textContent = pending;
    document.getElementById('statAnswered').textContent = answered;
    document.getElementById('statClosed').textContent = closed;
    document.getElementById('statTotal').textContent = allInquiries.length;
}

// ========================================
// 필터링
// ========================================
function filterInquiries(status) {
    currentFilter = status;
    
    // 필터 버튼 활성화
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 필터링
    if (status === 'ALL') {
        filteredInquiries = [...allInquiries];
    } else {
        filteredInquiries = allInquiries.filter(i => i.status === status);
    }
    
    displayInquiries();
}

// ========================================
// 문의 목록 표시
// ========================================
function displayInquiries() {
    const tbody = document.getElementById('inquiryTableBody');
    
    if (filteredInquiries.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 3rem; color: #7f8c8d;">
                    표시할 문의가 없습니다.
                </td>
            </tr>
        `;
        return;
    }
    
    // 최신순 정렬
    filteredInquiries.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    
    tbody.innerHTML = filteredInquiries.map((inquiry, index) => `
        <tr onclick="showInquiryDetail(${inquiry.inquiryId})">
            <td>${filteredInquiries.length - index}</td>
            <td><strong>${inquiry.title}</strong></td>
            <td>User #${inquiry.userId}</td>
            <td>
                <span class="status-badge ${inquiry.status.toLowerCase()}">
                    ${getStatusText(inquiry.status)}
                </span>
            </td>
            <td>${formatDate(inquiry.createdAt)}</td>
        </tr>
    `).join('');
}

// ========================================
// 상태 텍스트
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
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}.${month}.${day} ${hours}:${minutes}`;
}

// ========================================
// 문의 상세 모달
// ========================================
async function showInquiryDetail(inquiryId) {
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
        
        const modalContent = document.getElementById('modalContent');
        
        modalContent.innerHTML = `
            <div class="inquiry-info">
                <div style="display: flex; justify-content: space-between; margin-bottom: 1rem;">
                    <div>
                        <strong>작성자:</strong> User #${selectedInquiry.userId}
                    </div>
                    <div>
                        <strong>작성일:</strong> ${formatDate(selectedInquiry.createdAt)}
                    </div>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h3>${selectedInquiry.title}</h3>
                    <span class="status-badge ${selectedInquiry.status.toLowerCase()}">
                        ${getStatusText(selectedInquiry.status)}
                    </span>
                </div>
            </div>
            
            <div style="margin-bottom: 2rem;">
                <h4 style="margin-bottom: 1rem; color: #2c3e50;">문의 내용</h4>
                <div class="inquiry-content">
                    ${selectedInquiry.content.replace(/\n/g, '<br>')}
                </div>
            </div>
            
            ${selectedInquiry.reply ? `
                <div style="margin-bottom: 2rem;">
                    <h4 style="margin-bottom: 1rem; color: #27ae60;">✅ 작성된 답변</h4>
                    <div style="background: #e8f5e9; padding: 1.5rem; border-radius: 8px; border-left: 4px solid #27ae60;">
                        ${selectedInquiry.reply.replace(/\n/g, '<br>')}
                    </div>
                </div>
            ` : ''}
            
            ${selectedInquiry.status === 'PENDING' ? `
                <div class="reply-section">
                    <h4 style="margin-bottom: 1rem; color: #2c3e50;">💬 답변 작성</h4>
                    <div class="reply-form">
                        <textarea id="replyText" placeholder="답변 내용을 입력하세요..."></textarea>
                        <div style="display: flex; gap: 1rem; justify-content: flex-end;">
                            <button class="btn btn-secondary" onclick="closeModal()">취소</button>
                            <button class="btn btn-success" onclick="submitReply(${selectedInquiry.inquiryId})">답변 제출</button>
                        </div>
                    </div>
                </div>
            ` : `
                <div style="text-align: right;">
                    <button class="btn btn-secondary" onclick="closeModal()">닫기</button>
                </div>
            `}
        `;
        
        document.getElementById('inquiryModal').style.display = 'flex';
        
    } catch (error) {
        console.error('❌ 문의 상세 로드 실패:', error);
        alert('문의 내용을 불러오는데 실패했습니다.');
    }
}

// ========================================
// 답변 제출
// ========================================
async function submitReply(inquiryId) {
    const replyText = document.getElementById('replyText').value.trim();
    
    if (!replyText) {
        alert('답변 내용을 입력해주세요.');
        return;
    }
    
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`http://localhost:8080/api/inquiries/${inquiryId}/reply`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ reply: replyText })
        });
        
        if (!response.ok) {
            throw new Error('답변 제출 실패');
        }
        
        alert('✅ 답변이 성공적으로 제출되었습니다!');
        closeModal();
        await loadAllInquiries();
        
    } catch (error) {
        console.error('❌ 답변 제출 오류:', error);
        alert('❌ 답변 제출에 실패했습니다.');
    }
}

// ========================================
// 모달 닫기
// ========================================
function closeModal() {
    document.getElementById('inquiryModal').style.display = 'none';
    selectedInquiry = null;
}
