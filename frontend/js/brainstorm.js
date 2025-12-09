// ========================================
// 브레인스토밍 JavaScript (Python API 연동)
// ========================================

// 설정 - config.js에서 가져옴
const API_BASE_URL = CONFIG.PYTHON_API_BASE;

// 전역 변수
let sessionId = null;
let currentStep = 1;
let associations = [];
let waitingForResponse = false;

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('브레인스토밍 페이지 로드 완료');
    
    // 초기 메시지 제거 (HTML에서 지우고 동적으로 추가)
    const chatBox = document.getElementById('chatBox');
    chatBox.innerHTML = '';
    
    addMessage('ai', '어떤 아이디어가 필요하신가요?', false);
    addMessage('ai', '(예: 유튜브 컨텐츠 아이디어, 소상공인 마케팅 전략)', false);
    
    startSession();
});

// Step 1: 세션 시작
async function startSession() {
    try {
        showLoading('세션을 시작하는 중...');
        
        const response = await fetch(`${API_BASE_URL}/session`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('세션 시작 실패');
        }
        
        const data = await response.json();
        sessionId = data.session_id;
        
        console.log('✅ 세션 시작:', sessionId);
        
        hideLoading();
        updateProgress(2);
        
    } catch (error) {
        console.error('❌ 세션 시작 오류:', error);
        addMessage('ai', '세션 시작에 실패했습니다. 페이지를 새로고침해주세요.', true);
        hideLoading();
    }
}

// Step 2: 목적 입력 (Q1)
async function submitPurpose(purpose) {
    try {
        showLoading('목적을 설정하는 중...');
        
        const response = await fetch(`${API_BASE_URL}/purpose`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                session_id: sessionId,
                purpose: purpose
            })
        });
        
        if (!response.ok) {
            throw new Error('목적 입력 실패');
        }
        
        const data = await response.json();
        console.log('✅ 목적 입력 완료:', data);
        
        hideLoading();
        await getWarmupQuestions();
        
    } catch (error) {
        console.error('❌ 목적 입력 오류:', error);
        addMessage('ai', '목적 입력에 실패했습니다. 다시 시도해주세요.', true);
        hideLoading();
        waitingForResponse = false;
    }
}

// Step 3: 워밍업 질문 받기 (Q2)
async function getWarmupQuestions() {
    try {
        showLoading('워밍업 질문을 생성하는 중...');
        
        const response = await fetch(`${API_BASE_URL}/warmup/${sessionId}`);
        
        if (!response.ok) {
            throw new Error('워밍업 질문 생성 실패');
        }
        
        const data = await response.json();
        console.log('✅ 워밍업 질문:', data.questions);
        
        hideLoading();
        updateProgress(3);
        
        // 화면 클리어 후 워밍업 질문만 표시
        const chatBox = document.getElementById('chatBox');
        chatBox.innerHTML = '';
        
        // 질문들을 가운데 정렬로 표시
        data.questions.forEach((q, index) => {
            addMessage('ai', `${index + 1}. ${q}`, false);
        });
        
        addMessage('ai', '생각해보셨나요?', false);
        
        // 일반 입력창 숨기고 시작 버튼 표시
        document.getElementById('inputSection').style.display = 'none';
        
        // 시작 버튼 추가
        const startButtonHtml = `
            <div style="text-align: center; margin-top: 2rem;">
                <button class="start-button" onclick="confirmWarmup()" style="padding: 1rem 3rem; font-size: 1.1rem;">
                    🚀 시작하기
                </button>
            </div>
        `;
        chatBox.insertAdjacentHTML('beforeend', startButtonHtml);
        
        waitingForResponse = false;
        
    } catch (error) {
        console.error('❌ 워밍업 질문 오류:', error);
        addMessage('ai', '워밍업 질문 생성에 실패했습니다. 다시 시도해주세요.', true);
        hideLoading();
        waitingForResponse = false;
    }
}

// Step 3-2: 워밍업 확인
async function confirmWarmup() {
    try {
        showLoading('확인하는 중...');
        
        const response = await fetch(`${API_BASE_URL}/confirm/${sessionId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('워밍업 확인 실패');
        }
        
        console.log('✅ 워밍업 확인 완료');
        
        hideLoading();
        updateProgress(4);
        
        // 화면 클리어
        const chatBox = document.getElementById('chatBox');
        chatBox.innerHTML = '';
        
        // 자유연상 UI를 채팅 박스 안에 생성
        const associationUI = `
            <div style="text-align: center; margin-bottom: 2rem;">
                <p id="associationMessage" style="font-size: 1.2rem; font-weight: 600; color: #2c3e50; margin-bottom: 0.5rem;">
                    지금부터 떠오르는 무엇이든 자유롭게 많이 적어주세요.
                </p>
                <p style="font-size: 1rem; color: #7f8c8d;">현재 0개 입력</p>
            </div>
            <div class="association-tags-display" id="associationTagsBox" style="min-height: 150px; max-height: 300px; overflow-y: auto; padding: 1.5rem; background: white; border-radius: 12px; display: flex; flex-wrap: wrap; gap: 0.8rem; align-content: flex-start; margin-bottom: 2rem;">
                <!-- 태그가 여기에 추가됨 -->
            </div>
            <div style="text-align: center;">
                <button class="association-submit-button" id="submitAssociationsBtn" onclick="submitAssociations()" disabled style="width: 80%; max-width: 500px; background: #ccc; color: white; border: none; padding: 1.5rem; border-radius: 12px; font-size: 1.4rem; font-weight: 600; cursor: not-allowed;">
                    🎨 생성 (0개)
                </button>
            </div>
        `;
        chatBox.innerHTML = associationUI;
        
        // 아래 입력창 보이기
        document.getElementById('inputSection').style.display = 'flex';
        document.getElementById('associationInput').style.display = 'block';
        document.getElementById('userInput').style.display = 'none';
        document.getElementById('sendBtn').style.display = 'none';
        
        // 초기 상태 설정
        associations = [];  // 배열 초기화
        
        waitingForResponse = false;
        
    } catch (error) {
        console.error('❌ 워밍업 확인 오류:', error);
        addMessage('ai', '워밍업 확인에 실패했습니다.', true);
        hideLoading();
        waitingForResponse = false;
    }
}

// Step 4: 자유연상 키워드 추가
function addAssociation(keyword) {
    if (!keyword || keyword.trim() === '') return;
    
    keyword = keyword.trim();
    
    if (associations.includes(keyword)) {
        alert('이미 추가된 키워드입니다.');
        return;
    }
    
    associations.push(keyword);
    
    const tagsContainer = document.getElementById('associationTagsBox');
    const tag = document.createElement('span');
    tag.className = 'association-tag';
    tag.innerHTML = `
        ${keyword} 
        <button onclick="removeAssociation('${keyword}')">×</button>
    `;
    tagsContainer.appendChild(tag);
    
    document.getElementById('associationInput').value = '';
    updateAssociationButton();
    
    console.log('키워드 추가:', keyword, '총:', associations.length);
}

// 자유연상 키워드 제거
function removeAssociation(keyword) {
    associations = associations.filter(k => k !== keyword);
    
    const tagsContainer = document.getElementById('associationTagsBox');
    tagsContainer.innerHTML = '';
    associations.forEach(k => {
        const tag = document.createElement('span');
        tag.className = 'association-tag';
        tag.innerHTML = `
            ${k} 
            <button onclick="removeAssociation('${k}')">×</button>
        `;
        tagsContainer.appendChild(tag);
    });
    
    updateAssociationButton();
}

// 자유연상 버튼 상태 업데이트
function updateAssociationButton() {
    const button = document.getElementById('submitAssociationsBtn');
    const messageElem = document.getElementById('associationMessage');
    const count = associations.length;
    
    let message = '';
    let showButton = false;
    
    if (count < 5) {
        message = '지금부터 떠오르는 무엇이든 자유롭게 많이 적어주세요.';
        showButton = false;
    } else if (count >= 5 && count <= 9) {
        message = '😊 좋아요! 조금만 더 입력해볼까요?';
        showButton = false;
    } else if (count >= 10 && count <= 14) {
        message = '🎉 많이 입력했네요~! 더 있으면 입력하고, 없으면 \'생성\'을 눌러주세요';
        showButton = true;
    } else if (count >= 15 && count < 25) {
        message = '🚀 와! 많이 입력하셨네요! 준비되셨으면 \'생성\' 버튼을 눌러주세요';
        showButton = true;
    } else {
        message = '✅ 25개 입력 완료! 이제 아이디어를 생성해주세요 🎨';
        showButton = true;
    }
    
    if (messageElem) {
        messageElem.innerHTML = message;
        messageElem.nextElementSibling.textContent = `현재 ${count}개 입력`;
    }
    
    if (button) {
        button.innerHTML = `🎨 생성 (${count}개)`;
        button.disabled = !showButton;
        button.style.background = showButton ? '#759999' : '#ccc';
        button.style.cursor = showButton ? 'pointer' : 'not-allowed';
    }
}

// Step 4: 자유연상 제출
async function submitAssociations() {
    if (associations.length < 10) {
        alert('최소 10개 이상의 키워드를 입력해주세요.');
        return;
    }
    
    try {
        showLoading('자유연상을 분석하는 중...');
        
        const response = await fetch(`${API_BASE_URL}/associations/${sessionId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                session_id: sessionId,
                associations: associations
            })
        });
        
        if (!response.ok) {
            throw new Error('자유연상 제출 실패');
        }
        
        const data = await response.json();
        console.log('✅ 자유연상 제출 완료:', data);
        
        hideLoading();
        updateProgress(5);
        
        // 입력창 숨기기
        document.getElementById('inputSection').style.display = 'none';
        
        // 화면 클리어 후 생성 안내
        const chatBox = document.getElementById('chatBox');
        chatBox.innerHTML = '';
        
        // 아이디어 생성
        await generateIdeas();
        
    } catch (error) {
        console.error('❌ 자유연상 제출 오류:', error);
        addMessage('ai', '자유연상 제출에 실패했습니다. 다시 시도해주세요.', true);
        hideLoading();
    }
}

// Step 5: 아이디어 생성
async function generateIdeas() {
    try {
        showLoading('AI가 아이디어를 생성하는 중... (20초 정도 소요)');
        
        const response = await fetch(`${API_BASE_URL}/ideas/${sessionId}`);
        
        if (!response.ok) {
            throw new Error('아이디어 생성 실패');
        }
        
        const data = await response.json();
        console.log('✅ 아이디어 생성 완료:', data.ideas);
        
        hideLoading();
        
        // 완료 메시지를 아이디어 위에 추가
        const chatBox = document.getElementById('chatBox');
        const completeMessage = document.createElement('div');
        completeMessage.style.cssText = 'text-align: center; color: #2c3e50; margin: 2rem 0 3rem 0; font-size: 1.1rem; line-height: 1.8;';
        completeMessage.innerHTML = `
            🎉 아이디어 생성이 완료되었습니다!<br><br>
            아이디어를 저장하시려면 로그인이 필요합니다.<br>
            (현재는 임시 세션으로, 페이지를 닫으면 사라집니다)
        `;
        chatBox.appendChild(completeMessage);
        
        displayIdeas(data.ideas);
        
    } catch (error) {
        console.error('❌ 아이디어 생성 오류:', error);
        addMessage('ai', '아이디어 생성에 실패했습니다. 다시 시도해주세요.', true);
        hideLoading();
    }
}

// 아이디어 표시
function displayIdeas(ideas) {
    const chatBox = document.getElementById('chatBox');
    
    ideas.forEach((idea, index) => {
        let ideaHtml = `
            <div class="idea-result" onclick="toggleIdea(${index})">
                <h3>
                    <span>💡 아이디어 ${index + 1}: ${idea.title}</span>
                    <span class="idea-toggle collapsed" id="toggle-${index}">▶</span>
                </h3>
                <div class="idea-content" id="content-${index}">
                    <div class="idea-description">
                        ${idea.description.replace(/\n/g, '<br>')}
                    </div>
                    ${idea.analysis ? `
                    <div class="idea-analysis">
                        <h4>📊 분석 결과</h4>
                        <div style="color: #2c3e50;">
                            ${idea.analysis.replace(/\n/g, '<br>')}
                        </div>
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
        
        // 직접 채팅박스에 추가 (메시지 버블 없이)
        chatBox.insertAdjacentHTML('beforeend', ideaHtml);
    });
    
    // 저장 버튼 추가
    const saveButtonHtml = `
        <div style="text-align: center; margin: 3rem 0 2rem 0;">
            <button 
                class="save-ideas-button" 
                onclick="saveIdeas()" 
                style="padding: 1.2rem 3rem; font-size: 1.2rem; font-weight: 600; color: white; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; border-radius: 12px; cursor: pointer; transition: all 0.3s;"
                onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 12px rgba(102, 126, 234, 0.4)'"
                onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='none'"
            >
                💾 아이디어 저장하기
            </button>
            <p style="color: #7f8c8d; margin-top: 1rem; font-size: 0.95rem;">
                로그인하면 아이디어를 저장하고 나중에 다시 볼 수 있어요!
            </p>
        </div>
    `;
    chatBox.insertAdjacentHTML('beforeend', saveButtonHtml);
    
    // ideas를 전역 변수로 저장 (저장 시 사용)
    window.generatedIdeas = ideas;
}

// 아이디어 접기/펼치기
function toggleIdea(index) {
    const content = document.getElementById(`content-${index}`);
    const toggle = document.getElementById(`toggle-${index}`);
    
    if (content.classList.contains('expanded')) {
        content.classList.remove('expanded');
        toggle.classList.add('collapsed');
        toggle.textContent = '▶';
    } else {
        content.classList.add('expanded');
        toggle.classList.remove('collapsed');
        toggle.textContent = '▼';
    }
}

// 메시지 전송 (메인 로직)
async function sendMessage() {
    const input = document.getElementById('userInput');
    const message = input.value.trim();
    
    if (!message || waitingForResponse) return;
    
    // 사용자 메시지 표시
    addMessage('user', message, true);
    input.value = '';
    
    waitingForResponse = true;
    
    // 현재 단계에 따라 처리
    if (currentStep === 2) {
        // Q1: 목적 입력
        await submitPurpose(message);
    }
}

// Enter 키 처리
function handleEnter(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// 자유연상 Enter 키 처리
function handleAssociationEnter(event) {
    if (event.key === 'Enter') {
        const input = document.getElementById('associationInput');
        addAssociation(input.value);
    }
}

// 메시지 추가 함수
function addMessage(type, text, hasBackground = true) {
    const chatBox = document.getElementById('chatBox');
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    if (!hasBackground && type === 'ai') {
        messageDiv.classList.remove('has-background');
    } else if (hasBackground && type === 'ai') {
        messageDiv.classList.add('has-background');
    }
    
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.innerHTML = text;
    
    messageDiv.appendChild(bubble);
    chatBox.appendChild(messageDiv);
    
    chatBox.scrollTop = chatBox.scrollHeight;
}

// 로딩 표시
function showLoading(text) {
    const chatBox = document.getElementById('chatBox');
    
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'message ai';
    loadingDiv.id = 'loadingMessage';
    
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.innerHTML = `<span class="loading">${text}</span>`;
    
    loadingDiv.appendChild(bubble);
    chatBox.appendChild(loadingDiv);
    
    chatBox.scrollTop = chatBox.scrollHeight;
}

// 로딩 숨기기
function hideLoading() {
    const loadingMsg = document.getElementById('loadingMessage');
    if (loadingMsg) {
        loadingMsg.remove();
    }
}

// 진행 상태 업데이트
function updateProgress(step) {
    currentStep = step;
    
    for (let i = 1; i <= 5; i++) {
        const stepElement = document.getElementById(`step${i}`);
        if (i < step) {
            stepElement.classList.add('completed');
            stepElement.classList.remove('active');
        } else if (i === step) {
            stepElement.classList.add('active');
            stepElement.classList.remove('completed');
        } else {
            stepElement.classList.remove('active');
            stepElement.classList.remove('completed');
        }
    }
}

// 브레인스토밍 다시 시작
function resetBrainstorming() {
    if (confirm('브레인스토밍을 다시 시작하시겠습니까?')) {
        location.reload();
    }
}

// ========================================
// 아이디어 저장 (Spring Boot API)
// ========================================
async function saveIdeas() {
    // 1. 저장할 아이디어 확인
    if (!window.generatedIdeas || window.generatedIdeas.length === 0) {
        alert('저장할 아이디어가 없습니다.');
        return;
    }
    
    // 2. 로그인 확인
    const token = localStorage.getItem('token');
    
    if (!token) {
        // 비로그인: guestSessionId로 임시 저장
        await saveIdeasAsGuest();
        return;
    }
    
    // 3. 로그인 상태: 기존 방식으로 저장
    await saveIdeasAsUser(token);
}

// ========================================
// 비로그인 사용자: 게스트로 임시 저장
// ========================================
async function saveIdeasAsGuest() {
    try {
        showLoading('아이디어를 임시 저장하는 중...');
        
        // guestSessionId = Python 세션 ID 사용
        const guestSessionId = sessionId;
        
        // 각 아이디어를 guestSessionId로 저장
        const savePromises = window.generatedIdeas.map(async (idea, index) => {
            const ideaData = {
                userId: null,  // 비로그인이므로 null
                guestSessionId: guestSessionId,
                title: `${idea.title}`,
                content: JSON.stringify({
                    description: idea.description,
                    analysis: idea.analysis || '',
                    generatedAt: new Date().toISOString()
                }),
                purpose: sessionId || 'brainstorm_session'
            };
            
            const response = await fetch(`${CONFIG.SPRING_API_BASE}/ideas`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(ideaData)
            });
            
            if (!response.ok) {
                throw new Error(`아이디어 ${index + 1} 저장 실패`);
            }
            
            return await response.json();
        });
        
        await Promise.all(savePromises);
        
        // localStorage에 guestSessionId 저장 (로그인 후 연결용)
        localStorage.setItem('pendingGuestSessionId', guestSessionId);
        
        hideLoading();
        
        // Ephemeral RAG 세션 삭제
        await cleanupEphemeralSession();
        
        alert('✅ 아이디어가 임시 저장되었습니다!\n\n로그인하시면 "나의 아이디어"에서 확인할 수 있습니다.');
        
        // 로그인 페이지로 이동
        if (confirm('지금 로그인하시겠습니까?')) {
            location.href = 'index.html';
        }
        
    } catch (error) {
        console.error('❌ 아이디어 임시 저장 오류:', error);
        hideLoading();
        alert('❌ 아이디어 저장에 실패했습니다.\n\n' + error.message);
    }
}

// ========================================
// 로그인 사용자: userId로 저장
// ========================================
async function saveIdeasAsUser(token) {
    try {
        // 사용자 정보 가져오기
        const userResponse = await fetch(`${CONFIG.SPRING_API_BASE}/auth/me`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!userResponse.ok) {
            throw new Error('사용자 인증 실패');
        }
        
        const currentUser = await userResponse.json();
        
        showLoading('아이디어를 저장하는 중...');
        
        // 각 아이디어를 개별적으로 저장
        const savePromises = window.generatedIdeas.map(async (idea, index) => {
            const ideaData = {
                userId: currentUser.userId,
                guestSessionId: null,
                title: `${idea.title}`,
                content: JSON.stringify({
                    description: idea.description,
                    analysis: idea.analysis || '',
                    generatedAt: new Date().toISOString()
                }),
                purpose: sessionId || 'brainstorm_session'
            };
            
            const response = await fetch(`${CONFIG.SPRING_API_BASE}/ideas`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(ideaData)
            });
            
            if (!response.ok) {
                throw new Error(`아이디어 ${index + 1} 저장 실패`);
            }
            
            return await response.json();
        });
        
        await Promise.all(savePromises);
        
        hideLoading();
        
        // Ephemeral RAG 세션 삭제
        await cleanupEphemeralSession();
        
        alert('✅ 모든 아이디어가 저장되었습니다!\n\n홈 화면의 "나의 아이디어"에서 확인할 수 있습니다.');
        
        // 홈으로 이동
        location.href = 'index.html';
        
    } catch (error) {
        console.error('❌ 아이디어 저장 오류:', error);
        hideLoading();
        alert('❌ 아이디어 저장에 실패했습니다.\n\n' + error.message);
    }
}

// ========================================
// Ephemeral RAG 세션 정리
// ========================================
async function cleanupEphemeralSession() {
    try {
        console.log('🗑️ Ephemeral RAG 세션 삭제 시도...');
        const deleteResponse = await fetch(`${API_BASE_URL}/session/${sessionId}`, {
            method: 'DELETE'
        });
        
        if (deleteResponse.ok) {
            console.log('✅ Ephemeral RAG 세션 삭제 완료');
        } else {
            console.warn('⚠️ Ephemeral RAG 세션 삭제 실패 (무시)');
        }
    } catch (deleteError) {
        console.warn('⚠️ Ephemeral RAG 세션 삭제 오류 (무시):', deleteError);
    }
}
