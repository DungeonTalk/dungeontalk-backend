// 던전게임 전용 JavaScript
(() => {
    'use strict';
    
    // 게임 상태 변수
    let selectedWorld = null;
    let gameSessionStarted = false;
    let isMatching = false;
    let currentGameSession = null;
    let currentTurnNumber = 1;
    let aiGameRoomId = null;
    let partyRoomId = null;
    
    // 초기화
    window.addEventListener('load', function() {
        // 새로고침 시 로컬스토리지 초기화 (테스트 편의용)
        clearAuthData();
        console.log('🔄 새로고침 감지 - 로컬스토리지 초기화');
    });
    
    // 인증 데이터 초기화
    function clearAuthData() {
        AuthManager.clearAuthData();
        selectedWorld = null;
        gameSessionStarted = false;
        isMatching = false;
        currentGameSession = null;
        aiGameRoomId = null;
        partyRoomId = null;
        
        // WebSocket 연결 종료
        wsManager.disconnectAll();
        
        console.log('🗑️ 모든 캐시 데이터 삭제 완료');
    }
    
    // 로그인 상태 UI 업데이트
    function showLoggedInState() {
        Utils.toggleElement('loginSection', false);
        Utils.toggleElement('matchingSection', true);
        
        const currentUser = AuthManager.getCurrentUser();
        Utils.updateStatus('currentStatus', '매칭 대기');
        Utils.updateStatus('currentUser', currentUser.name);
        
        // WebSocket 연결
        const token = AuthManager.getAuthToken();
        if (token) {
            setTimeout(() => {
                connectMatchingWebSocket();
            }, 500);
        }
    }
    
    // 회원가입
    async function register() {
        const userId = document.getElementById('userId').value.trim();
        const password = document.getElementById('password').value.trim();
        
        if (!userId || !password) {
            GlobalNotification.error('ID와 비밀번호를 입력하세요.');
            return;
        }
        
        try {
            const { response, result } = await ApiClient.post('/v1/member/register', {
                name: userId,
                nickName: userId + '님',
                password: password
            }, false);
            
            if (response.ok) {
                GlobalNotification.success('회원가입 성공! 이제 로그인하세요.');
            } else {
                GlobalNotification.error('회원가입 실패: ' + (result.text || '알 수 없는 오류'));
            }
        } catch (error) {
            GlobalNotification.error('회원가입 오류: ' + error.message);
        }
    }
    
    // 로그인
    async function login() {
        const userId = document.getElementById('userId').value.trim();
        const password = document.getElementById('password').value.trim();
        
        if (!userId || !password) {
            GlobalNotification.error('ID와 비밀번호를 입력하세요.');
            return;
        }
        
        try {
            const { response, result } = await ApiClient.post('/v1/auth/login', {
                name: userId,
                password: password
            }, false);
            
            if (response.ok) {
                const loginData = result.data;
                
                // JWT 토큰 저장
                const user = {
                    id: loginData.memberId,
                    name: userId,
                    nickname: userId
                };
                
                AuthManager.setAuthData(loginData.accessToken, user);
                
                showLoggedInState();
                GlobalNotification.success('로그인 성공!');
            } else {
                GlobalNotification.error('로그인 실패: ' + (result.text || '알 수 없는 오류'));
            }
        } catch (error) {
            GlobalNotification.error('로그인 오류: ' + error.message);
        }
    }
    
    // 세계관 선택
    function selectWorld(worldType) {
        // 이전 선택 해제
        document.querySelectorAll('.world-card').forEach(card => {
            card.classList.remove('selected');
        });
        
        // 새로운 선택
        document.querySelector(`[data-world="${worldType}"]`).classList.add('selected');
        selectedWorld = worldType;
        
        // 매칭 시작 버튼 활성화
        document.getElementById('startMatchingBtn').disabled = false;
    }
    
    // 세계관 이름 변환
    function getWorldName(worldType) {
        const names = {
            'FANTASY': '🏰 판타지',
            'SF': '🚀 SF', 
            'MODERN': '🏙️ 현대'
        };
        return names[worldType] || worldType;
    }
    
    // 매칭 WebSocket 연결
    function connectMatchingWebSocket() {
        const token = AuthManager.getAuthToken();
        const currentUser = AuthManager.getCurrentUser();
        
        if (!token || !currentUser) {
            return;
        }
        
        const url = `/ws-chat?token=${encodeURIComponent(token)}&roomId=matching`;
        
        wsManager.connect('matching', url, 
            (stompClient, frame) => {
                // 개인 매칭 알림 구독
                const subscriptionPath = `/sub/matching/user/${currentUser.id}`;
                console.log('매칭 알림 구독:', subscriptionPath);
                
                wsManager.subscribe('matching', subscriptionPath, (message) => {
                    console.log('매칭 메시지 수신:', message.body);
                    handleMatchingMessage(JSON.parse(message.body));
                });
            },
            null,
            (error) => {
                console.error('매칭 WebSocket 연결 실패:', error);
            }
        );
    }
    
    // 매칭 시작
    async function startMatching() {
        if (!selectedWorld) {
            GlobalNotification.error('세계관을 선택해주세요.');
            return;
        }
        
        const currentUser = AuthManager.getCurrentUser();
        if (!AuthManager.isAuthenticated() || !currentUser) {
            GlobalNotification.error('로그인이 필요합니다.');
            return;
        }
        
        try {
            const { response, result } = await ApiClient.post('/api/v1/match/join', {
                memberId: currentUser.id,
                worldType: selectedWorld
            });
            
            if (response.ok && result.resultCode === '200') {
                isMatching = true;
                Utils.toggleElement('startMatchingBtn', false);
                Utils.toggleElement('cancelMatchingBtn', true);
                Utils.toggleElement('matchingStatus', true);
                
                Utils.updateStatus('matchingInfo', `${getWorldName(selectedWorld)} 매칭 중`);
                GlobalNotification.success('매칭이 시작되었습니다!');
            } else {
                GlobalNotification.error('매칭 시작 실패: ' + (result.msg || '알 수 없는 오류'));
            }
        } catch (error) {
            GlobalNotification.error('매칭 오류: ' + error.message);
        }
    }
    
    // 매칭 취소
    async function cancelMatching() {
        const currentUser = AuthManager.getCurrentUser();
        
        try {
            const { response, result } = await ApiClient.delete('/api/v1/match/cancel', {
                memberId: currentUser.id
            });
            
            if (response.ok && result.resultCode === '200') {
                isMatching = false;
                Utils.toggleElement('startMatchingBtn', true);
                Utils.toggleElement('cancelMatchingBtn', false);
                Utils.toggleElement('matchingStatus', false);
                
                Utils.updateStatus('matchingInfo', '대기 중');
                GlobalNotification.info('매칭이 취소되었습니다.');
            } else {
                GlobalNotification.error('매칭 취소 실패: ' + (result.msg || '알 수 없는 오류'));
            }
        } catch (error) {
            GlobalNotification.error('매칭 취소 오류: ' + error.message);
        }
    }
    
    // 매칭 메시지 처리
    function handleMatchingMessage(message) {
        switch (message.type) {
            case 'QUEUE_STATUS_UPDATE':
                updateQueueStatus(message.data);
                break;
            case 'MATCHING_COMPLETE':
                handleMatchingComplete(message.data);
                break;
            case 'MATCHING_CANCELLED':
                handleMatchingCancelled();
                break;
            case 'ERROR':
                console.error('매칭 오류:', message.data);
                GlobalNotification.error('매칭 오류: ' + message.data);
                break;
        }
    }
    
    // 큐 상태 업데이트
    function updateQueueStatus(data) {
        const queueInfoElement = document.getElementById('queueInfo');
        if (queueInfoElement) {
            queueInfoElement.innerHTML = `
                대기 순서: ${data.currentPosition}번째 | 
                총 대기자: ${data.totalInQueue}명 | 
                예상 시간: ${data.estimatedMessage}
            `;
        }
    }
    
    // 매칭 완료 처리
    function handleMatchingComplete(data) {
        console.log('=== 매칭 완료 데이터 ===', data);
        
        isMatching = false;
        currentGameSession = data;
        aiGameRoomId = data.aiGameRoomId;
        partyRoomId = data.chatRoomId;
        
        // UI 전환
        Utils.toggleElement('matchingSection', false);
        Utils.toggleElement('gameSection', true);
        
        // 상태 업데이트
        Utils.updateStatus('currentStatus', '게임 중');
        Utils.updateStatus('matchingInfo', '매칭 완료');
        Utils.updateStatus('roomInfo', `AI: ${aiGameRoomId.substring(0, 8)}... | 파티: ${partyRoomId.substring(0, 8)}...`);
        
        // 참여자 표시
        updateParticipantsList(data.participants);
        
        // 매칭 완료 알림
        GlobalNotification.success('매칭 완료! 게임이 시작됩니다.');
        
        // 게임 시작
        startGameSession(data);
    }
    
    // 매칭 취소 처리
    function handleMatchingCancelled() {
        isMatching = false;
        Utils.toggleElement('startMatchingBtn', true);
        Utils.toggleElement('cancelMatchingBtn', false);
        Utils.toggleElement('matchingStatus', false);
        Utils.updateStatus('matchingInfo', '대기 중');
    }
    
    // 게임 세션 시작
    async function startGameSession(matchData) {
        try {
            // 중복 요청 방지
            if (gameSessionStarted) {
                console.log('🔄 게임 세션이 이미 시작됨');
                return;
            }
            
            if (!AuthManager.isAuthenticated()) {
                throw new Error('인증 토큰이 필요합니다. 다시 로그인해주세요.');
            }
            
            gameSessionStarted = true;
            console.log('🔑 AI 게임 세션 시작 요청:', aiGameRoomId);
            
            // AI 게임방 세션 시작
            const { response, result } = await ApiClient.post(`/api/v1/aichat/rooms/${aiGameRoomId}/start`);
            
            if (response.ok && result.resultCode === '200') {
                console.log('AI 게임 세션 시작 성공');
                
                // WebSocket 연결들
                connectAiWebSocket();
                connectPartyWebSocket();
                
                // 입력 활성화
                enableChatInputs();
                
                // 환영 메시지
                addAiMessage('system', `🌟 ${getWorldName(selectedWorld)} 세계에 오신 것을 환영합니다!`);
                addAiMessage('system', '🤖 AI 마스터가 여러분의 모험을 안내할 것입니다.');
                addAiMessage('ai', `안녕하세요 모험가들! 저는 여러분의 게임 마스터 AI입니다. ${getWorldName(selectedWorld)} 세계에서 펼쳐진 모험을 함께 만들어갑시다. 먼저 자기소개를 해주세요!`);
                
                addUserMessage('system', '💬 플레이어들과의 채팅이 시작되었습니다!');
                addUserMessage('system', `참여자: ${matchData.participants.join(', ')}`);
                
            } else {
                console.error('❌ AI 게임 세션 시작 실패:', result);
                
                if (response.status === 401) {
                    GlobalNotification.error('인증이 만료되었습니다. 다시 로그인해주세요.');
                    clearAuthData();
                } else {
                    GlobalNotification.error('AI 게임 세션 시작 실패: ' + (result.msg || '알 수 없는 오류'));
                }
                gameSessionStarted = false;
            }
        } catch (error) {
            console.error('❌ 게임 세션 시작 오류:', error);
            gameSessionStarted = false;
            if (error.message.includes('인증')) {
                clearAuthData();
            } else {
                GlobalNotification.error('게임 세션 시작 오류: ' + error.message);
            }
        }
    }
    
    // AI WebSocket 연결
    function connectAiWebSocket() {
        const token = AuthManager.getAuthToken();
        
        if (!token || !aiGameRoomId) {
            console.error('AI WebSocket 연결 실패: 토큰 또는 룸ID 없음');
            return;
        }
        
        const url = `/ws-chat?token=${encodeURIComponent(token)}&roomId=${encodeURIComponent(aiGameRoomId)}`;
        
        wsManager.connect('ai', url,
            (stompClient, frame) => {
                console.log('🤖 AI 채팅 WebSocket 연결 성공');
                
                // AI 채팅방 구독
                wsManager.subscribe('ai', `/sub/aichat/room/${aiGameRoomId}`, (message) => {
                    const aiMessage = JSON.parse(message.body);
                    handleAiMessage(aiMessage);
                });
                
                console.log('✅ AI 채팅방 구독 완료:', `/sub/aichat/room/${aiGameRoomId}`);
            },
            null,
            (error) => {
                console.error('❌ AI 채팅 WebSocket 연결 실패:', error);
            }
        );
    }
    
    // 파티 WebSocket 연결
    function connectPartyWebSocket() {
        const token = AuthManager.getAuthToken();
        
        if (!token || !partyRoomId) {
            console.error('파티 WebSocket 연결 실패: 토큰 또는 룸ID 없음');
            return;
        }
        
        const url = `/ws-chat?token=${encodeURIComponent(token)}&roomId=${encodeURIComponent(partyRoomId)}`;
        
        wsManager.connect('party', url,
            (stompClient, frame) => {
                console.log('👥 파티 채팅 WebSocket 연결 성공');
                
                // 사용자 채팅방 구독
                wsManager.subscribe('party', `/sub/chat/room/${partyRoomId}`, (message) => {
                    const userMessage = JSON.parse(message.body);
                    handleUserMessage(userMessage);
                });
                
                console.log('✅ 파티 채팅방 구독 완료:', `/sub/chat/room/${partyRoomId}`);
            },
            null,
            (error) => {
                console.error('❌ 파티 채팅 WebSocket 연결 실패:', error);
            }
        );
    }
    
    // AI 메시지 처리
    function handleAiMessage(message) {
        const currentUser = AuthManager.getCurrentUser();
        
        // 자신의 메시지인지 확인
        const isMyMessage = message.messageType === 'USER' && message.senderId === currentUser.id;
        
        if (isMyMessage) {
            console.log('✅ 자신의 메시지 필터링');
            return;
        }
        
        if (message.messageType === 'AI') {
            addAiMessage('ai', message.content);
        } else if (message.messageType === 'USER') {
            addAiMessage('other', `${message.senderNickname}: ${message.content}`);
        } else if (message.messageType === 'SYSTEM') {
            addAiMessage('system', message.content);
        }
    }
    
    // 사용자 메시지 처리
    function handleUserMessage(message) {
        const currentUser = AuthManager.getCurrentUser();
        
        // 자신이 보낸 메시지는 이미 화면에 표시했으므로 무시
        if (message.senderId === currentUser.id) {
            return;
        }
        
        if (message.type === 'TALK') {
            const nickname = message.senderNickname || message.senderId || 'Unknown';
            addUserMessage('other', `${nickname}: ${message.content}`);
        } else if (message.type === 'ENTER' || message.type === 'LEAVE') {
            addUserMessage('system', message.content);
        }
    }
    
    // AI 메시지 전송
    function sendAiMessage() {
        const input = document.getElementById('aiMessageInput');
        const message = input.value.trim();
        
        if (!message) return;
        
        // 내 메시지 표시
        addAiMessage('user', message);
        
        // WebSocket으로 전송
        const currentUser = AuthManager.getCurrentUser();
        const success = wsManager.send('ai', '/pub/aichat/send', {
            aiGameRoomId: aiGameRoomId,
            gameId: currentGameSession.gameSessionId,
            senderId: currentUser.id,
            senderNickname: currentUser.name,
            content: message,
            messageType: 'USER',
            turnNumber: getCurrentTurnNumber()
        });
        
        if (success) {
            input.value = '';
        }
    }
    
    // AI 응답 요청
    async function requestAiResponse() {
        if (!aiGameRoomId || !currentGameSession) {
            GlobalNotification.error('게임 세션이 유효하지 않습니다.');
            return;
        }
        
        const currentUser = AuthManager.getCurrentUser();
        const requestData = {
            gameId: currentGameSession.gameSessionId,
            currentUser: currentUser.name,
            currentMessage: "사용자 행동 완료",
            turnNumber: getCurrentTurnNumber()
        };
        
        try {
            const response = await fetch(`/api/v1/aichat/ai-service/rooms/${aiGameRoomId}/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            const data = await response.json();
            
            if (data.resultCode !== '200-1') {
                GlobalNotification.error('AI 응답 요청 실패: ' + data.msg);
            } else {
                console.log('✅ AI 응답 생성 성공');
                GlobalNotification.success('AI 응답이 생성되었습니다.');
            }
        } catch (error) {
            console.error('AI 응답 요청 오류:', error);
            GlobalNotification.error('AI 응답 요청 중 오류가 발생했습니다.');
        }
    }
    
    // 사용자 메시지 전송
    function sendUserMessage() {
        const input = document.getElementById('userMessageInput');
        const message = input.value.trim();
        
        if (!message) return;
        
        // 내 메시지 표시
        addUserMessage('user', message);
        
        // WebSocket으로 전송
        const currentUser = AuthManager.getCurrentUser();
        const success = wsManager.send('party', '/pub/chat/send', {
            roomId: partyRoomId,
            senderId: currentUser.id,
            senderNickname: currentUser.name,
            content: message,
            type: 'TALK'
        });
        
        if (success) {
            input.value = '';
        }
    }
    
    // 탭 전환
    function switchTab(tab) {
        // 탭 버튼 업데이트
        document.querySelectorAll('.chat-tab').forEach(t => t.classList.remove('active'));
        
        if (tab === 'ai') {
            document.getElementById('aiTab').classList.add('active');
            Utils.toggleElement('aiChat', true);
            Utils.toggleElement('userChat', false);
        } else if (tab === 'user') {
            document.getElementById('userTab').classList.add('active');
            Utils.toggleElement('aiChat', false);
            Utils.toggleElement('userChat', true);
        }
    }
    
    // 채팅 입력 활성화
    function enableChatInputs() {
        const inputs = ['aiMessageInput', 'aiSendBtn', 'aiRequestBtn', 'userMessageInput', 'userSendBtn'];
        inputs.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.disabled = false;
            }
        });
    }
    
    // AI 메시지 추가
    function addAiMessage(type, content) {
        const messagesDiv = document.getElementById('aiMessages');
        if (!messagesDiv) return;
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.innerHTML = Utils.formatMessage(content);
        
        messagesDiv.appendChild(messageDiv);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
    
    // 사용자 메시지 추가
    function addUserMessage(type, content) {
        const messagesDiv = document.getElementById('userMessages');
        if (!messagesDiv) return;
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.innerHTML = Utils.formatMessage(content);
        
        messagesDiv.appendChild(messageDiv);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
    
    // 참여자 목록 업데이트
    function updateParticipantsList(participants) {
        const listDiv = document.getElementById('participantsList');
        if (!listDiv) return;
        
        listDiv.innerHTML = '';
        const currentUser = AuthManager.getCurrentUser();
        
        participants.forEach(participant => {
            const participantDiv = document.createElement('div');
            participantDiv.className = 'participant';
            if (participant === currentUser.id) {
                participantDiv.classList.add('current-user');
            }
            participantDiv.textContent = participant;
            listDiv.appendChild(participantDiv);
        });
    }
    
    // 현재 턴 번호 가져오기
    function getCurrentTurnNumber() {
        return currentTurnNumber;
    }
    
    // 턴 번호 증가
    function incrementTurnNumber() {
        currentTurnNumber++;
    }
    
    // 전역 함수로 노출 (HTML에서 호출)
    window.DungeonGame = {
        register,
        login,
        selectWorld,
        startMatching,
        cancelMatching,
        sendAiMessage,
        requestAiResponse,
        sendUserMessage,
        switchTab
    };
    
})();