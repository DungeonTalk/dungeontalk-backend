        // 전역 변수
        let authToken = null;
        let currentUser = null;
        let selectedWorld = null;
        let gameSessionStarted = false;
        let matchingStompClient = null;
        let aiStompClient = null;
        let partyStompClient = null;
        let isMatching = false;
        let currentGameSession = null;
        let currentTurnNumber = 1;
        let aiGameRoomId = null;
        let partyRoomId = null;
        let participantNicknames = new Set(); // 참여자 닉네임 수집용
        let characterStats = null; // 캐릭터 스탯 정보
        
        // 15분 TRPG 타이머 변수
        let gameStartTime = null;
        let timerInterval = null;
        let targetGameDuration = 15; // 15분
        let currentGamePhase = 'waiting';
        let gamePhasesData = {
            waiting: { name: '준비 중...', pressure: 'relaxed' },
            intro: { name: '🌅 도입', pressure: 'relaxed' },
            development: { name: '⚡ 전개', pressure: 'normal' },
            middle: { name: '🔥 중반', pressure: 'normal' },
            climax: { name: '💥 클라이맥스', pressure: 'urgent' },
            ending: { name: '🎭 종료', pressure: 'critical' }
        };
        
        // 초기화
        window.addEventListener('load', function() {
            // 새로고침 시 로컬스토리지 초기화 (테스트 편의용)
            clearAuthData();
            console.log('🔄 새로고침 감지 - 로컬스토리지 초기화');
        });
        
        // 기존 로그인 확인
        function checkExistingLogin() {
            authToken = localStorage.getItem('authToken');
            const userInfo = localStorage.getItem('currentUser');
            
            console.log('인증 체크:', { 
                authToken: authToken ? ('존재: ' + authToken.substring(0, 10) + '...') : 'null', 
                userInfo: userInfo ? '존재' : 'null' 
            });
            
            if (authToken && authToken !== 'null' && authToken.trim() !== '' && userInfo) {
                try {
                    currentUser = JSON.parse(userInfo);
                    console.log('✅ 로그인 상태 확인:', currentUser.name);
                    showLoggedInState();
                } catch (e) {
                    console.error('❌ 사용자 정보 파싱 오류:', e);
                    clearAuthData();
                }
            } else {
                console.log('⚠️ 로그인 필요');
                clearAuthData();
            }
        }
        
        function clearAuthData() {
            authToken = null;
            currentUser = null;
            selectedWorld = null;
            gameSessionStarted = false;
            isMatching = false;
            currentGameSession = null;
            aiGameRoomId = null;
            partyRoomId = null;
            localStorage.clear(); // 모든 로컬스토리지 데이터 삭제
            
            // WebSocket 연결 종료
            if (matchingStompClient && matchingStompClient.connected) {
                matchingStompClient.disconnect();
                matchingStompClient = null;
            }
            
            console.log('🗑️ 모든 캐시 데이터 삭제 완료');
        }
        
        // 로그인 상태 UI 업데이트
        function showLoggedInState() {
            document.getElementById('loginSection').classList.add('hidden');
            document.getElementById('matchingSection').classList.remove('hidden');
            document.getElementById('headerActions').style.display = 'flex';
            
            updateStatus('currentStatus', '매칭 대기');
            updateStatus('currentUser', currentUser.name);
            
            // authToken이 설정된 후에 WebSocket 연결
            if (authToken) {
                setTimeout(() => {
                    connectMatchingWebSocket();
                }, 500); // 약간의 지연을 두어 토큰 설정 완료 후 연결
            }
        }
        
        // 회원가입
        async function register() {
            const userId = document.getElementById('userId').value.trim();
            const password = document.getElementById('password').value.trim();
            
            if (!userId || !password) {
                alert('ID와 비밀번호를 입력하세요.');
                return;
            }
            
            console.log('회원가입 시도:', { name: userId, nickName: userId });
            
            try {
                const response = await fetch('/v1/member/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: userId,
                        nickName: userId,
                        password: password
                    })
                });
                
                console.log('회원가입 응답 상태:', response.status);
                
                if (response.ok) {
                    const result = await response.json();
                    console.log('회원가입 성공:', result);
                    alert('회원가입 성공! 이제 로그인하세요.');
                } else {
                    const errorText = await response.text();
                    console.error('회원가입 실패:', errorText);
                    try {
                        const errorJson = JSON.parse(errorText);
                        alert('회원가입 실패: ' + (errorJson.msg || errorJson.message || errorText));
                    } catch (e) {
                        alert('회원가입 실패: ' + errorText);
                    }
                }
            } catch (error) {
                alert('회원가입 오류: ' + error.message);
            }
        }
        
        // 로그인
        async function login() {
            const userId = document.getElementById('userId').value.trim();
            const password = document.getElementById('password').value.trim();
            
            if (!userId || !password) {
                alert('ID와 비밀번호를 입력하세요.');
                return;
            }
            
            try {
                const response = await fetch('/v1/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: userId,
                        password: password
                    })
                });
                
                if (response.ok) {
                    const responseData = await response.json();
                    const loginData = responseData.data;
                    
                    console.log('로그인 응답 데이터:', loginData);
                    
                    // JWT 토큰 저장
                    authToken = loginData.accessToken;
                    
                    // JWT 토큰에서 사용자 정보 추출
                    currentUser = extractUserInfoFromToken(authToken);
                    
                    if (!currentUser) {
                        console.error('JWT 토큰에서 사용자 정보를 추출할 수 없습니다.');
                        alert('로그인 오류: 사용자 정보를 가져올 수 없습니다.');
                        return;
                    }
                    
                    console.log('JWT에서 추출한 사용자 정보:', currentUser);
                    
                    localStorage.setItem('authToken', authToken);
                    localStorage.setItem('currentUser', JSON.stringify(currentUser));
                    
                    // 토큰 설정 완료 후 UI 업데이트
                    showLoggedInState();
                } else {
                    const error = await response.text();
                    alert('로그인 실패: ' + error);
                }
            } catch (error) {
                alert('로그인 오류: ' + error.message);
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
            
            // 배경 테마 적용
            applyWorldTheme(worldType);
            
            // 세계관 정보 표시
            updateWorldInfo(worldType);
            
            // 매칭 시작 버튼 활성화
            document.getElementById('startMatchingBtn').disabled = false;
            
            console.log(`✨ ${getWorldName(worldType)} 세계관 선택됨`);
        }
        
        function getWorldName(worldType) {
            const names = {
                'FANTASY': '🏰 판타지',
                'ZOMBIE': '🧟 좀비'
            };
            return names[worldType] || worldType;
        }
        
        // 세계관 테마 적용
        function applyWorldTheme(worldType) {
            // 기존 테마 클래스 제거
            document.body.classList.remove('theme-fantasy', 'theme-zombie');
            
            // 새 테마 클래스 추가
            switch(worldType) {
                case 'FANTASY':
                    document.body.classList.add('theme-fantasy');
                    updateAiChatInfo('🏰 중세 판타지 세계에서의 모험이 시작됩니다! 마법과 검의 세계에서 용감한 행동을 취해보세요.');
                    updatePlaceholders('마법사에게 말을 걸어본다', '동료들과 전략을 논의해보세요');
                    break;
                case 'ZOMBIE':
                    document.body.classList.add('theme-zombie');
                    updateAiChatInfo('🧟 좀비 아포칼립스 세계에서 살아남으세요! 생존을 위해 자원을 찾고 위험을 피하세요.');
                    updatePlaceholders('주변에서 물품을 찾아본다', '생존자들과 계획을 세우세요');
                    break;
            }
        }
        
        // 세계관 정보 업데이트 (게임상태 창이 제거되어 빈 함수로 유지)
        function updateWorldInfo(worldType) {
            // 게임상태 창이 제거되어 더 이상 표시할 곳이 없음
            console.log(`세계관 선택됨: ${getWorldName(worldType)}`);
        }
        
        // AI 채팅 정보 업데이트
        function updateAiChatInfo(message) {
            const aiChatInfo = document.getElementById('aiChatInfo');
            if (aiChatInfo) {
                aiChatInfo.textContent = message;
            }
        }
        
        // 플레이스홀더 업데이트
        function updatePlaceholders(aiPlaceholder, userPlaceholder) {
            const aiInput = document.getElementById('aiMessageInput');
            const rightUserInput = document.getElementById('rightUserMessageInput');
            
            if (aiInput) {
                aiInput.placeholder = `${aiPlaceholder} (예시 행동)`;
            }
            if (rightUserInput) {
                rightUserInput.placeholder = userPlaceholder;
            }
        }
        
        // 매칭 WebSocket 연결
        function connectMatchingWebSocket() {
            if (matchingStompClient && matchingStompClient.connected) {
                return;
            }
            
            if (!authToken) {
                return;
            }
            
            try {
                console.log('매칭 WebSocket 연결 시도 중...', '/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=matching');
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=matching');
                matchingStompClient = Stomp.over(socket);
                
                matchingStompClient.connect({}, function(frame) {
                    console.log('✅ 매칭 WebSocket 연결 성공:', frame);
                    
                    // 개인 매칭 알림 구독
                    const subscriptionPath = `/sub/matching/user/${currentUser.id}`;
                    console.log('매칭 알림 구독:', subscriptionPath);
                    matchingStompClient.subscribe(subscriptionPath, function(message) {
                        console.log('매칭 메시지 수신:', message.body);
                        handleMatchingMessage(JSON.parse(message.body));
                    });
                    
                }, function(error) {
                    console.error('❌ 매칭 WebSocket 연결 실패:', error);
                    console.log('연결 URL:', '/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=matching');
                });
            } catch (error) {
                console.error('매칭 WebSocket 오류:', error);
            }
        }
        
        // 매칭 시작
        async function startMatching() {
         /*   if (!selectedWorld) {
                alert('세계관을 선택해주세요.');
                return;
            }*/
            
            if (!authToken || authToken === 'null') {
                console.error('❌ JWT 토큰이 없음');
                alert('로그인이 필요합니다.');
                return;
            }
            
            if (!currentUser || !currentUser.id) {
                console.error('❌ 사용자 정보가 없음');
                alert('로그인 정보가 유효하지 않습니다.');
                return;
            }
            
            // 캐릭터 존재 여부 체크
            try {
                const hasCharacter = await checkCharacterExists(currentUser.id);
                if (!hasCharacter) {
                    alert('캐릭터가 없습니다. 먼저 "⚔️ 내 캐릭터" 버튼을 클릭하여 캐릭터를 생성해주세요.');
                    return;
                }
            } catch (error) {
                console.error('캐릭터 체크 실패:', error);
                alert('캐릭터 정보 확인 중 오류가 발생했습니다.');
                return;
            }
            
            try {
                console.log('매칭 시작 API 호출:', '/v1/match/join', { memberId: currentUser.id, worldType: selectedWorld });
                const response = await fetch('/v1/match/join', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    },
                    body: JSON.stringify({
                        memberId: currentUser.id,
                        worldTypeCode: selectedWorld
                    })
                });
                
                console.log('API 응답 상태:', response.status, response.statusText);
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                
                const result = await response.json();
                console.log('매칭 시작 API 응답:', result);
                if (result.resultCode === '200') {
                    isMatching = true;
                    document.getElementById('startMatchingBtn').classList.add('hidden');
                    document.getElementById('cancelMatchingBtn').classList.remove('hidden');
                    document.getElementById('matchingStatus').classList.remove('hidden');
                    
                    updateStatus('matchingInfo', `${getWorldName(selectedWorld)} 매칭 중`);
                } else {
                    alert('매칭 시작 실패: ' + (result.msg || '알 수 없는 오류'));
                }
            } catch (error) {
                alert('매칭 오류: ' + error.message);
            }
        }
        
        // 매칭 취소
        async function cancelMatching() {
            try {
                const response = await fetch('/v1/match/cancel', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    },
                    body: JSON.stringify({
                        memberId: currentUser.id
                    })
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                
                const result = await response.json();
                if (result.resultCode === '200') {
                    isMatching = false;
                    document.getElementById('startMatchingBtn').classList.remove('hidden');
                    document.getElementById('cancelMatchingBtn').classList.add('hidden');
                    document.getElementById('matchingStatus').classList.add('hidden');
                    
                    updateStatus('matchingInfo', '대기 중');
                } else {
                    alert('매칭 취소 실패: ' + (result.msg || '알 수 없는 오류'));
                }
            } catch (error) {
                alert('매칭 취소 오류: ' + error.message);
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
                    alert('매칭 오류: ' + message.data);
                    break;
            }
        }
        
        // 큐 상태 업데이트
        function updateQueueStatus(data) {
            document.getElementById('queueInfo').innerHTML = `
                대기 순서: ${data.currentPosition}번째 | 
                총 대기자: ${data.totalInQueue}명 | 
                예상 시간: ${data.estimatedMessage}
            `;
        }
        
        // 매칭 완료 처리
        function handleMatchingComplete(data) {
            console.log('=== 매칭 완료 데이터 디버깅 ===');
            console.log('data:', data);
            console.log('data.aiGameRoomId:', data.aiGameRoomId);
            console.log('data.chatRoomId:', data.chatRoomId);
            console.log('data.gameSessionId:', data.gameSessionId);
            console.log('==============================');
            
            isMatching = false;
            currentGameSession = data;
            aiGameRoomId = data.aiGameRoomId;
            partyRoomId = data.chatRoomId;
            
            // UI 전환
            document.getElementById('matchingSection').classList.add('hidden');
            document.getElementById('gameSection').classList.remove('hidden');
            
            // 상태 업데이트
            updateStatus('currentStatus', '게임 중');
            updateStatus('matchingInfo', '매칭 완료');
            updateStatus('roomInfo', `AI: ${aiGameRoomId.substring(0, 8)}... | 파티: ${partyRoomId.substring(0, 8)}...`);
            
            // 헤더에 퇴장 버튼 표시
            document.getElementById('leaveGameHeaderBtn').classList.remove('hidden');
            
            // 참여자 표시 제거됨
            
            // 게임 시작
            startGameSession(data);
        }
        
        // 매칭 취소 처리
        function handleMatchingCancelled() {
            isMatching = false;
            document.getElementById('startMatchingBtn').classList.remove('hidden');
            document.getElementById('cancelMatchingBtn').classList.add('hidden');
            document.getElementById('matchingStatus').classList.add('hidden');
            updateStatus('matchingInfo', '대기 중');
        }
        
        // 게임 세션 시작
        async function startGameSession(matchData) {
            try {
                // 중복 요청 방지
                if (gameSessionStarted) {
                    console.log('🔄 게임 세션이 이미 시작됨, 중복 요청 무시');
                    return;
                }
                
                // JWT 토큰 유효성 검사
                if (!authToken || authToken === 'null' || authToken.trim() === '') {
                    console.error('❌ JWT 토큰이 없거나 유효하지 않음:', authToken);
                    throw new Error('인증 토큰이 필요합니다. 다시 로그인해주세요.');
                }
                
                gameSessionStarted = true;
                console.log('🔑 JWT 토큰 확인됨, AI 게임 세션 시작 요청:', aiGameRoomId);
                
                // AI 게임방 세션 시작
                const sessionResponse = await fetch(`/v1/rooms/ai/${aiGameRoomId}/start`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + authToken
                    }
                });
                
                const sessionResult = await sessionResponse.json();
                if (sessionResponse.ok && sessionResult.resultCode === '200') {
                    console.log('AI 게임 세션 시작 성공');
                    
                    // WebSocket 연결들
                    connectAiWebSocket();
                    connectPartyWebSocket();
                    
                    // 캐릭터 스탯 조회 (게임 시작 시 한번만)
                    await loadCharacterStats();
                    
                    // 입력 활성화
                    enableChatInputs();
                    
                    // 세계관별 환영 메시지
                    addWorldSpecificWelcomeMessages(selectedWorld);
                    
                    addRightUserMessage('system', '💬 플레이어들과의 채팅이 시작되었습니다!');
                    
                    // 참여자 수 표시 (닉네임 대신 간단하게)
                    const participantCount = matchData.participants ? matchData.participants.length : 1;
                    addRightUserMessage('system', `👥 총 ${participantCount}명의 모험가가 함께합니다!`);
                    
                } else {
                    console.error('❌ AI 게임 세션 시작 실패:', {
                        status: sessionResponse.status,
                        statusText: sessionResponse.statusText,
                        result: sessionResult
                    });
                    
                    if (sessionResponse.status === 401) {
                        alert('인증이 만료되었습니다. 다시 로그인해주세요.');
                        clearAuthData();
                        showLoginModal();
                    } else {
                        alert('AI 게임 세션 시작 실패: ' + (sessionResult.msg || '알 수 없는 오류'));
                    }
                    gameSessionStarted = false; // 실패 시 플래그 리셋
                }
            } catch (error) {
                console.error('❌ 게임 세션 시작 오류:', error);
                gameSessionStarted = false; // 에러 시 플래그 리셋
                if (error.message.includes('인증')) {
                    clearAuthData();
                    showLoginModal();
                } else {
                    alert('게임 세션 시작 오류: ' + error.message);
                }
            }
        }
        
        // AI WebSocket 연결
        function connectAiWebSocket() {
            console.log('=== AI WebSocket 연결 시도 ===');
            console.log('authToken:', authToken ? 'exists' : 'null');
            console.log('aiGameRoomId:', aiGameRoomId);
            console.log('currentGameSession:', currentGameSession);
            
            // 기존 AI WebSocket 연결 해제
            if (aiStompClient && aiStompClient.connected) {
                console.log('🔄 기존 AI WebSocket 연결 해제 중...');
                try {
                    aiStompClient.disconnect();
                    console.log('✅ 기존 AI WebSocket 연결 해제 완료');
                } catch (error) {
                    console.error('❌ 기존 AI WebSocket 해제 오류:', error);
                }
            }
            
            if (!authToken) {
                console.error('AI WebSocket 연결 실패: 인증 토큰이 없습니다.');
                return;
            }
            
            if (!aiGameRoomId) {
                console.error('AI WebSocket 연결 실패: aiGameRoomId가 없습니다.');
                return;
            }
            
            console.log('================================');
            
            try {
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=' + encodeURIComponent(aiGameRoomId));
                aiStompClient = Stomp.over(socket);
                
                aiStompClient.connect({}, function(frame) {
                    console.log('🤖 AI 채팅 WebSocket 연결 성공');
                    
                    // AI 채팅방 구독 (단일 구독)
                    const subscription = aiStompClient.subscribe(`/sub/aichat/room/${aiGameRoomId}`, function(message) {
                        console.log('=== AI WebSocket 메시지 수신 ===');
                        console.log('Raw message:', message);
                        console.log('Message body:', message.body);
                        const aiMessage = JSON.parse(message.body);
                        console.log('Parsed aiMessage:', aiMessage);
                        console.log('handleAiMessage 호출 전');
                        handleAiMessage(aiMessage);
                        console.log('handleAiMessage 호출 후');
                        console.log('=========================================');
                    });
                    
                    console.log('✅ AI 채팅방 구독 완료:', `/sub/aichat/room/${aiGameRoomId}`);
                    
                }, function(error) {
                    console.error('❌ AI 채팅 WebSocket 연결 실패:', error);
                });
            } catch (error) {
                console.error('❌ AI WebSocket 오류:', error);
            }
        }
        
        // 파티 WebSocket 연결
        function connectPartyWebSocket() {
            console.log('=== 파티 WebSocket 연결 시도 ===');
            
            // 기존 파티 WebSocket 연결 해제
            if (partyStompClient && partyStompClient.connected) {
                console.log('🔄 기존 파티 WebSocket 연결 해제 중...');
                try {
                    partyStompClient.disconnect();
                    console.log('✅ 기존 파티 WebSocket 연결 해제 완료');
                } catch (error) {
                    console.error('❌ 기존 파티 WebSocket 해제 오류:', error);
                }
            }
            
            if (!authToken) {
                console.error('파티 WebSocket 연결 실패: 인증 토큰이 없습니다.');
                return;
            }
            
            if (!partyRoomId) {
                console.error('파티 WebSocket 연결 실패: partyRoomId가 없습니다.');
                return;
            }
            
            try {
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=' + encodeURIComponent(partyRoomId));
                partyStompClient = Stomp.over(socket);
                
                partyStompClient.connect({}, function(frame) {
                    console.log('👥 파티 채팅 WebSocket 연결 성공');
                    
                    // 사용자 채팅방 구독 (단일 구독)
                    const subscription = partyStompClient.subscribe(`/sub/chat/room/${partyRoomId}`, function(message) {
                        const userMessage = JSON.parse(message.body);
                        handleUserMessage(userMessage);
                    });
                    
                    console.log('✅ 파티 채팅방 구독 완료:', `/sub/chat/room/${partyRoomId}`);
                    
                }, function(error) {
                    console.error('❌ 파티 채팅 WebSocket 연결 실패:', error);
                });
            } catch (error) {
                console.error('❌ 파티 WebSocket 오류:', error);
            }
        }
        
        // AI 메시지 처리
        function handleAiMessage(message) {
            // 모든 메시지에 대해 디버깅 로그 출력
            console.log('=== AI 메시지 디버깅 ===');
            console.log('message.senderId:', message.senderId);
            console.log('message.senderId type:', typeof message.senderId);
            console.log('currentUser.id:', currentUser.id);
            console.log('currentUser.id type:', typeof currentUser.id);
            console.log('ID 비교 결과:', message.senderId === currentUser.id);
            console.log('message.messageType:', message.messageType);
            console.log('message.content:', message.content);
            console.log('message.senderNickname:', message.senderNickname);
            
            // 자신의 메시지인지 확인
            const isMyMessage = message.messageType === 'USER' && message.senderId === currentUser.id;
            console.log('자신의 메시지인가?', isMyMessage);
            
            if (isMyMessage) {
                console.log('✅ 자신의 메시지 필터링 - 화면에 추가 안 함');
                console.log('==========================');
                return;
            }
            
            console.log('✅ 다른 사람 메시지 - 화면에 추가');
            console.log('==========================');
            
            if (message.messageType === 'AI') {
                // AI 응답이 도착하면 대기 상태 해제
                endAiWaiting();
                addAiMessage('ai', message.content);
                
                // AI 메시지에서 [GAME_END] 키워드 감지
                if (message.content && message.content.includes('[GAME_END]')) {
                    console.log('🎮 AI 응답에서 [GAME_END] 키워드 감지!');
                    // [GAME_END] 키워드를 제거한 메시지로 게임 종료 처리
                    const cleanMessage = {
                        ...message,
                        messageType: 'GAME_END',
                        content: message.content.replace(/\[GAME_END\]/g, '').trim()
                    };
                    // 약간의 지연 후 게임 종료 처리 (사용자가 AI 메시지를 읽을 시간 제공)
                    setTimeout(() => {
                        handleGameEndMessage(cleanMessage);
                    }, 2000);
                }
            } else if (message.messageType === 'USER') {
                addAiMessage('other', `${message.senderNickname}: ${message.content}`);
            } else if (message.messageType === 'SYSTEM') {
                addAiMessage('system', message.content);
                
                // AI 요청 시작 메시지를 받으면 모든 플레이어 대기 상태 시작
                if (message.content && message.content.includes('🤖 AI가 응답을 생성하고 있습니다')) {
                    startAiWaiting();
                }
                // 실패/에러 메시지만 대기 상태 해제 (완료 메시지는 AI 응답이 올 때까지 대기)
                else if (message.content && (
                    message.content.includes('❌ AI 응답 요청에 실패했습니다') ||
                    message.content.includes('❌ AI 응답 요청 중 오류가 발생했습니다')
                )) {
                    setTimeout(() => {
                        endAiWaiting();
                    }, 3000);
                }
            } else if (message.messageType === 'GAME_END') {
                handleGameEndMessage(message);
            }
        }
        
        // 게임 종료 메시지 처리
        function handleGameEndMessage(message) {
            console.log('🎮 게임 종료 메시지 수신:', message);
            
            // 게임 결과 분석
            const gameResult = analyzeGameResult(message.content);
            
            // 게임 종료 메시지 표시
            addAiMessage('system', getGameEndSystemMessage(gameResult));
            if (message.content && message.content.trim()) {
                addAiMessage('ai', message.content);
            }
            
            // 타이머 정지
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
                handleGameTimeEnd();
            }
            
            // 입력 비활성화
            disableChatInputs();
            
            // 상태 업데이트
            updateStatus('currentStatus', '🔴 게임 종료');
            updateStatus('roomInfo', '🎯 게임 완료');
            
            // 타이머 영역에 게임 종료 표시
            const timerElement = document.getElementById('timerDisplay');
            if (timerElement) {
                timerElement.innerHTML = '⏹️ 게임 종료';
                timerElement.style.cssText = `
                    color: #ff4757;
                    font-weight: bold;
                    font-size: 18px;
                    background: linear-gradient(135deg, #ff6b6b, #ff4757);
                    background-clip: text;
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                `;
            }
            
            // 매칭 버튼들 숨기기
            const startMatchingBtn = document.getElementById('startMatchingBtn');
            const cancelMatchingBtn = document.getElementById('cancelMatchingBtn');
            if (startMatchingBtn) startMatchingBtn.style.display = 'none';
            if (cancelMatchingBtn) cancelMatchingBtn.style.display = 'none';
            
            // 게임 종료 상태 메시지 표시
            const matchingStatus = document.getElementById('matchingStatus');
            if (matchingStatus) {
                matchingStatus.innerHTML = '🎭 게임이 완료되었습니다';
                matchingStatus.style.cssText = `
                    color: #ff4757;
                    font-weight: bold;
                    font-size: 16px;
                    text-align: center;
                    padding: 10px;
                    background: rgba(255, 107, 107, 0.1);
                    border-radius: 8px;
                    border: 2px solid #ff6b6b;
                `;
                matchingStatus.classList.remove('hidden');
            }
            
            // 상단에 큰 게임 종료 배너 표시
            showGameEndBanner(gameResult);
            
            // 페이지 제목 변경
            document.title = '🎭 게임 종료 - 던전톡';
            
            // 게임 종료 효과 추가
            showGameEndEffect(message.content);
        }
        
        // 게임 결과 분석
        function analyzeGameResult(content) {
            if (!content) return 'UNKNOWN';
            
            const lowerContent = content.toLowerCase();
            
            // 성공 키워드들
            const successKeywords = [
                '성공', '승리', '완료', '클리어', '달성', '해결', '구출', '탈출',
                'success', 'victory', 'complete', 'clear', 'achieve', 'win'
            ];
            
            // 실패 키워드들
            const failureKeywords = [
                '실패', '패배', '전멸', '게임오버', '죽었', '쓰러졌', '불가능',
                'failure', 'defeat', 'dead', 'died', 'game over', 'impossible'
            ];
            
            // 시간 초과 키워드들
            const timeoutKeywords = [
                '시간', '초과', '타임', '종료', 'time', 'timeout', 'expired'
            ];
            
            // 키워드 우선순위로 판단
            if (successKeywords.some(keyword => lowerContent.includes(keyword))) {
                return 'SUCCESS';
            } else if (failureKeywords.some(keyword => lowerContent.includes(keyword))) {
                return 'FAILURE';
            } else if (timeoutKeywords.some(keyword => lowerContent.includes(keyword))) {
                return 'TIMEOUT';
            }
            
            return 'UNKNOWN';
        }
        
        // 게임 종료 시스템 메시지 생성
        function getGameEndSystemMessage(gameResult) {
            const messages = {
                'SUCCESS': '🎉 축하합니다! 게임이 성공적으로 클리어되었습니다!',
                'FAILURE': '💀 게임이 종료되었습니다. 아쉽지만 다음 기회에 도전해보세요!',
                'TIMEOUT': '⏰ 시간이 초과되어 게임이 종료되었습니다!',
                'UNKNOWN': '🎭 게임이 종료되었습니다!'
            };
            
            return messages[gameResult] || messages['UNKNOWN'];
        }
        
        // 게임 종료 배너 표시
        function showGameEndBanner(gameResult = 'UNKNOWN') {
            // 기존 배너가 있으면 제거
            const existingBanner = document.getElementById('gameEndBanner');
            if (existingBanner) {
                existingBanner.remove();
            }
            
            // 결과에 따른 배너 설정
            const bannerConfigs = {
                'SUCCESS': {
                    gradient: 'linear-gradient(135deg, #28a745, #20c997)',
                    text: '🎉 게임 클리어! 축하합니다! 🎉',
                    icon: '🏆'
                },
                'FAILURE': {
                    gradient: 'linear-gradient(135deg, #dc3545, #fd7e14)',
                    text: '💀 게임 오버! 다음에 다시 도전하세요! 💀',
                    icon: '⚰️'
                },
                'TIMEOUT': {
                    gradient: 'linear-gradient(135deg, #ffc107, #fd7e14)',
                    text: '⏰ 시간 초과! 게임이 종료되었습니다! ⏰',
                    icon: '⏱️'
                },
                'UNKNOWN': {
                    gradient: 'linear-gradient(135deg, #ff6b6b, #feca57)',
                    text: '🎭 게임이 종료되었습니다! 🎭',
                    icon: '🎮'
                }
            };
            
            const config = bannerConfigs[gameResult] || bannerConfigs['UNKNOWN'];
            
            // 게임 종료 배너 생성
            const banner = document.createElement('div');
            banner.id = 'gameEndBanner';
            banner.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                background: ${config.gradient};
                color: white;
                text-align: center;
                padding: 15px;
                font-size: 24px;
                font-weight: bold;
                z-index: 9999;
                box-shadow: 0 4px 8px rgba(0,0,0,0.3);
                animation: gameEndSlide 1s ease-out;
            `;
            banner.innerHTML = `
                ${config.icon} <span style="text-shadow: 2px 2px 4px rgba(0,0,0,0.5);">${config.text}</span> ${config.icon}
            `;
            
            // CSS 애니메이션 추가
            if (!document.getElementById('gameEndStyles')) {
                const styles = document.createElement('style');
                styles.id = 'gameEndStyles';
                styles.innerHTML = `
                    @keyframes gameEndSlide {
                        from { transform: translateY(-100%); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    @keyframes gameEndPulse {
                        0%, 100% { transform: scale(1); }
                        50% { transform: scale(1.05); }
                    }
                    .game-ended-overlay {
                        background: rgba(0, 0, 0, 0.7) !important;
                        pointer-events: none;
                    }
                `;
                document.head.appendChild(styles);
            }
            
            // 배너를 페이지 상단에 추가
            document.body.insertBefore(banner, document.body.firstChild);
            
            // 전체 페이지에 게임 종료 오버레이 효과
            document.body.classList.add('game-ended-overlay');
            
            // 5초 후 배너 자동 제거 (선택사항)
            setTimeout(() => {
                banner.style.animation = 'gameEndSlide 1s ease-in reverse';
                setTimeout(() => {
                    if (banner.parentNode) {
                        banner.remove();
                    }
                }, 1000);
            }, 5000);
        }
        
        // 게임 종료 효과 표시
        function showGameEndEffect(finalMessage) {
            // 배경 효과
            document.body.style.filter = 'brightness(0.8)';
            
            // 종료 메시지에서 결과 파싱
            let resultType = 'UNKNOWN';
            if (finalMessage.includes('성공') || finalMessage.includes('승리') || finalMessage.includes('완료')) {
                resultType = 'SUCCESS';
            } else if (finalMessage.includes('실패') || finalMessage.includes('패배') || finalMessage.includes('게임오버')) {
                resultType = 'FAILURE';
            } else if (finalMessage.includes('시간') || finalMessage.includes('초과')) {
                resultType = 'TIMEOUT';
            }
            
            // 결과에 따른 메시지 추가
            setTimeout(() => {
                switch (resultType) {
                    case 'SUCCESS':
                        addAiMessage('system', '🎉 축하합니다! 미션을 성공적으로 완료했습니다!');
                        break;
                    case 'FAILURE':
                        addAiMessage('system', '💀 아쉽게도 이번 모험은 여기서 끝납니다...');
                        break;
                    case 'TIMEOUT':
                        addAiMessage('system', '⏰ 시간이 부족했지만 좋은 모험이었습니다!');
                        break;
                    default:
                        addAiMessage('system', '🎭 모험이 마무리되었습니다. 수고하셨습니다!');
                }
                
                // 게임 재시작 버튼 표시 (선택사항)
                addAiMessage('system', '새로운 모험을 시작하려면 페이지를 새로고침하세요.');
            }, 2000);
        }

        // 사용자 메시지 처리
        function handleUserMessage(message) {
            // 자신이 보낸 메시지는 이미 화면에 표시했으므로 무시
            if (message.senderId === currentUser.id) {
                return;
            }
            
            if (message.type === 'TALK') {
                const nickname = message.senderNickname || message.senderId || 'Unknown';
                
                // 닉네임 수집 (자신 제외)
                if (nickname !== 'Unknown' && nickname !== currentUser.name) {
                    participantNicknames.add(nickname);
                    updateParticipantsList();
                }
                
                addRightUserMessage('other', `${nickname}: ${message.content}`);
            } else if (message.type === 'ENTER') {
                // 입장 메시지에서도 닉네임 추출
                const nickname = extractNicknameFromEnterMessage(message.content);
                if (nickname && nickname !== currentUser.name) {
                    participantNicknames.add(nickname);
                    updateParticipantsList();
                }
                addRightUserMessage('system', message.content);
            } else if (message.type === 'LEAVE') {
                // 퇴장 메시지에서 닉네임 제거
                const nickname = extractNicknameFromLeaveMessage(message.content);
                if (nickname) {
                    participantNicknames.delete(nickname);
                    updateParticipantsList();
                }
                addRightUserMessage('system', message.content);
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
            if (aiStompClient && aiStompClient.connected) {
                aiStompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                    roomId: aiGameRoomId,
                    roomType: 'AI_GAME',
                    senderId: currentUser.id,
                    messageType: 'USER',
                    content: message,
                    aiGameRoomId: aiGameRoomId,
                    gameActionType: 'CHAT',
                    turnNumber: getCurrentTurnNumber(),
                    characterStats: characterStats // 캐릭터 스탯 포함
                }));
            }
            
            input.value = '';
        }
        

        // AI 응답 요청 (REST API 호출) - 기존 방식
        function requestAiResponse() {
            if (!aiGameRoomId || !currentUser || !currentGameSession) {
                alert('게임 세션이 유효하지 않습니다.');
                return;
            }
            
            // AI 응답 대기 상태는 WebSocket 메시지를 받을 때 시작됨
            
            const requestData = {
                gameId: currentGameSession.gameSessionId,
                currentUser: currentUser.name,
                currentMessage: "사용자 행동 완료",
                turnNumber: getCurrentTurnNumber(),
                gameStartTime: gameStartTime ? Math.floor(gameStartTime / 1000) : null,
                targetDuration: targetGameDuration,
                characterStats: characterStats // 캐릭터 스탯 포함
            };
            
            console.log('AI 응답 요청 시작:', requestData);
            
            // 모든 플레이어에게 AI 응답 요청 시작 알림 (WebSocket으로 브로드캐스트)
            if (aiStompClient && aiStompClient.connected) {
                aiStompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                    roomId: aiGameRoomId,
                    roomType: 'AI_GAME',
                    senderId: 'SYSTEM',
                    messageType: 'SYSTEM',
                    content: '🤖 AI가 응답을 생성하고 있습니다... 잠시만 기다려주세요.',
                    aiGameRoomId: aiGameRoomId,
                    gameActionType: 'AI_REQUEST_START'
                }));
            }
            
            fetch(`/v1/rooms/ai/${aiGameRoomId}/ai/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + authToken
                },
                body: JSON.stringify(requestData)
            })
            .then(response => response.json())
            .then(data => {
                console.log('AI 응답 요청 완료:', data);
                if (data.resultCode !== '200-1') {
                    alert('AI 응답 요청 실패: ' + data.msg);
                    // 실패 메시지를 모든 플레이어에게 브로드캐스트
                    if (aiStompClient && aiStompClient.connected) {
                        aiStompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                            roomId: aiGameRoomId,
                            roomType: 'AI_GAME',
                            senderId: 'SYSTEM',
                            messageType: 'SYSTEM',
                            content: '❌ AI 응답 요청에 실패했습니다.',
                            aiGameRoomId: aiGameRoomId,
                            gameActionType: 'AI_REQUEST_FAILED'
                        }));
                    }
                } else {
                    console.log('✅ AI 응답 생성 성공');
                    // AI 응답 요청 성공 시 별도 메시지 없이 AI 응답 자체를 기다림
                    // (실제 AI 응답이 도착하면 그것이 완료 신호가 됨)
                }
            })
            .catch(error => {
                console.error('AI 응답 요청 오류:', error);
                alert('AI 응답 요청 중 오류가 발생했습니다.');
                // 에러 메시지를 모든 플레이어에게 브로드캐스트
                if (aiStompClient && aiStompClient.connected) {
                    aiStompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                        roomId: aiGameRoomId,
                        roomType: 'AI_GAME',
                        senderId: 'SYSTEM',
                        messageType: 'SYSTEM',
                        content: '❌ AI 응답 요청 중 오류가 발생했습니다.',
                        aiGameRoomId: aiGameRoomId,
                        gameActionType: 'AI_REQUEST_ERROR'
                    }));
                }
            });
            // 대기 상태 해제는 WebSocket 메시지를 통해 처리됨
        }
        
        // AI 응답 대기 상태 시작
        function startAiWaiting() {
            const aiPanel = document.querySelector('.chat-panel:first-child');
            const aiInput = document.getElementById('aiMessageInput');
            const aiSendBtn = document.getElementById('aiSendBtn');
            const aiRequestBtn = document.getElementById('aiRequestBtn');
            
            // AI 패널에 대기 상태 클래스 추가
            if (aiPanel) {
                aiPanel.classList.add('ai-waiting');
                
                // 로딩 인디케이터 추가
                const loadingIndicator = document.createElement('div');
                loadingIndicator.className = 'ai-loading-indicator';
                loadingIndicator.innerHTML = `
                    <div class="ai-loading-spinner"></div>
                    <span>AI 응답 생성 중...</span>
                `;
                aiPanel.appendChild(loadingIndicator);
            }
            
            // 입력 요소들 비활성화
            if (aiInput) aiInput.disabled = true;
            if (aiSendBtn) aiSendBtn.disabled = true;
            if (aiRequestBtn) aiRequestBtn.disabled = true;
        }
        
        // AI 응답 대기 상태 종료
        function endAiWaiting() {
            const aiPanel = document.querySelector('.chat-panel:first-child');
            const aiInput = document.getElementById('aiMessageInput');
            const aiSendBtn = document.getElementById('aiSendBtn');
            const aiRequestBtn = document.getElementById('aiRequestBtn');
            
            // AI 패널에서 대기 상태 클래스 제거
            if (aiPanel) {
                aiPanel.classList.remove('ai-waiting');
                
                // 로딩 인디케이터 제거
                const loadingIndicator = aiPanel.querySelector('.ai-loading-indicator');
                if (loadingIndicator) {
                    loadingIndicator.remove();
                }
            }
            
            // 입력 요소들 활성화
            if (aiInput) aiInput.disabled = false;
            if (aiSendBtn) aiSendBtn.disabled = false;
            if (aiRequestBtn) aiRequestBtn.disabled = false;
        }
        
        
        // 오른쪽 패널 사용자 메시지 전송
        function sendRightUserMessage() {
            const input = document.getElementById('rightUserMessageInput');
            const message = input.value.trim();
            
            if (!message) return;
            
            // 내 메시지 표시
            addRightUserMessage('user', message);
            
            // WebSocket으로 전송
            if (partyStompClient && partyStompClient.connected) {
                partyStompClient.send('/pub/room/chat/send', {}, JSON.stringify({
                    roomId: partyRoomId,
                    roomType: 'PLAYER_CHAT',
                    senderId: currentUser.id,
                    messageType: 'USER',
                    content: message,
                    chatRoomId: partyRoomId,
                    senderNickname: currentUser.name
                }));
            }
            
            input.value = '';
        }
        
        // 캐릭터 스탯 조회 (멤버ID로 캐릭터 찾은 후 상세 정보 조회)
        async function fetchCharacterStats(memberId) {
            if (!memberId || !authToken) {
                console.warn('⚠️ 멤버 ID 또는 인증 토큰이 없음');
                return null;
            }
            
            try {
                console.log('🎮 캐릭터 스탯 조회 시작 (멤버ID):', memberId);
                
                // 1단계: 멤버ID로 캐릭터 기본 정보 조회
                const basicResponse = await fetch(`/v1/characters?memberId=${memberId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (!basicResponse.ok) {
                    console.warn('⚠️ 캐릭터 기본 정보 조회 실패:', basicResponse.status);
                    return null;
                }
                
                const basicResult = await basicResponse.json();
                if (basicResult.resultCode !== '200' || !basicResult.data) {
                    console.warn('⚠️ 캐릭터 기본 정보 응답 오류:', basicResult);
                    return null;
                }
                
                const characterId = basicResult.data.id;
                console.log('✅ 캐릭터 기본 정보 조회 성공, 캐릭터 ID:', characterId);
                
                // 2단계: 캐릭터ID로 상세 정보 조회 (계산된 스탯 포함)
                const detailResponse = await fetch(`/v1/characters/${characterId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Content-Type': 'application/json'
                    }
                });
                
                if (detailResponse.ok) {
                    const detailResult = await detailResponse.json();
                    if (detailResult.resultCode === '200' && detailResult.data) {
                        console.log('✅ 캐릭터 상세 스탯 조회 성공:', detailResult.data);
                        return detailResult.data;
                    } else {
                        console.warn('⚠️ 캐릭터 상세 정보 응답 오류:', detailResult);
                        return null;
                    }
                } else {
                    console.warn('⚠️ 캐릭터 상세 정보 API 호출 실패:', detailResponse.status);
                    return null;
                }
            } catch (error) {
                console.error('❌ 캐릭터 스탯 조회 오류:', error);
                return null;
            }
        }
        
        // 캐릭터 스탯 로드 및 저장
        async function loadCharacterStats() {
            try {
                // 현재 사용자의 멤버 ID 사용
                const memberId = currentUser.id;
                
                console.log('🎯 캐릭터 스탯 로딩 시작 (멤버ID)...', memberId);
                characterStats = await fetchCharacterStats(memberId);
                
                if (characterStats) {
                    console.log('✅ 캐릭터 스탯 로딩 완료:', characterStats);
                    
                    // 스탯 로딩 성공 메시지 (AI 채팅에 표시)
                    addAiMessage('system', '⚔️ 캐릭터 정보가 AI에게 전달되었습니다. 스탯 기반 게임이 시작됩니다!');
                    
                    // 간단한 스탯 요약 표시
                    if (characterStats.raceName) {
                        addAiMessage('system', `🎮 캐릭터: ${characterStats.raceName} ${characterStats.level || 1}레벨`);
                    }
                } else {
                    console.warn('⚠️ 캐릭터 스탯 로딩 실패 - 기본 게임으로 진행');
                    addAiMessage('system', '⚠️ 캐릭터 정보를 불러올 수 없어 기본 게임으로 진행됩니다.');
                }
            } catch (error) {
                console.error('❌ 캐릭터 스탯 로딩 오류:', error);
                addAiMessage('system', '⚠️ 캐릭터 정보 로딩 중 오류가 발생했습니다.');
            }
        }
        
        // 입장 메시지에서 닉네임 추출
        function extractNicknameFromEnterMessage(content) {
            // "닉네임님이 입장했습니다" 형태에서 닉네임 추출
            const match = content.match(/(.+)님이?\s*입장했습니다?/);
            return match ? match[1].trim() : null;
        }
        
        // 퇴장 메시지에서 닉네임 추출
        function extractNicknameFromLeaveMessage(content) {
            // "닉네임님이 퇴장했습니다" 형태에서 닉네임 추출
            const match = content.match(/(.+)님이?\s*퇴장했습니다?/);
            return match ? match[1].trim() : null;
        }
        
        // 참여자 목록 업데이트 (중복 방지)
        let lastParticipantUpdate = '';
        function updateParticipantsList() {
            const participantNames = Array.from(participantNicknames);
            if (participantNames.length > 0) {
                // 자신 포함해서 표시
                const allParticipants = [currentUser.name, ...participantNames];
                const currentList = allParticipants.join(', ');
                
                // 이전과 다를 때만 업데이트
                if (currentList !== lastParticipantUpdate) {
                    addRightUserMessage('system', `👥 현재 모험가: ${currentList} (총 ${allParticipants.length}명)`);
                    lastParticipantUpdate = currentList;
                }
            }
        }
        
        // 게임 나가기 함수
        async function leaveGame() {
            if (!confirm('정말로 게임을 나가시겠습니까? 진행 중인 게임이 종료됩니다.')) {
                return;
            }
            
            try {
                console.log('🚪 게임 나가기 시작...');
                
                // 1. AI 게임방 퇴장
                if (aiGameRoomId && authToken) {
                    try {
                        const aiLeaveResponse = await fetch(`/v1/aichat/rooms/${aiGameRoomId}/leave?participantId=${currentUser.id}`, {
                            method: 'POST',
                            headers: {
                                'Authorization': 'Bearer ' + authToken,
                                'Content-Type': 'application/json'
                            }
                        });
                        
                        if (aiLeaveResponse.ok) {
                            console.log('✅ AI 게임방 퇴장 성공');
                        } else {
                            console.warn('⚠️ AI 게임방 퇴장 실패:', aiLeaveResponse.status);
                        }
                    } catch (error) {
                        console.warn('⚠️ AI 게임방 퇴장 오류:', error);
                    }
                }
                
                // 2. 파티 채팅방 퇴장
                if (partyRoomId && authToken) {
                    try {
                        const partyLeaveResponse = await fetch(`/v1/chat/room/${partyRoomId}/leave/${currentUser.id}`, {
                            method: 'DELETE',
                            headers: {
                                'Authorization': 'Bearer ' + authToken,
                                'Content-Type': 'application/json'
                            }
                        });
                        
                        if (partyLeaveResponse.ok) {
                            console.log('✅ 파티 채팅방 퇴장 성공');
                        } else {
                            console.warn('⚠️ 파티 채팅방 퇴장 실패:', partyLeaveResponse.status);
                        }
                    } catch (error) {
                        console.warn('⚠️ 파티 채팅방 퇴장 오류:', error);
                    }
                }
                
                // 3. WebSocket 퇴장 메시지 전송
                if (aiStompClient && aiStompClient.connected) {
                    try {
                        aiStompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                            roomId: aiGameRoomId,
                            senderId: currentUser.id,
                            messageType: 'LEAVE',
                            content: '게임을 떠났습니다.',
                            roomType: 'AI_GAME'
                        }));
                    } catch (error) {
                        console.warn('⚠️ AI WebSocket 퇴장 메시지 전송 오류:', error);
                    }
                }
                
                if (partyStompClient && partyStompClient.connected) {
                    try {
                        partyStompClient.send('/pub/room/chat/send', {}, JSON.stringify({
                            roomId: partyRoomId,
                            senderId: currentUser.id,
                            type: 'LEAVE',
                            content: '게임을 떠났습니다.',
                            roomType: 'PLAYER_CHAT'
                        }));
                    } catch (error) {
                        console.warn('⚠️ 파티 WebSocket 퇴장 메시지 전송 오류:', error);
                    }
                }
                
                // 4. WebSocket 연결 해제
                disconnectAllWebSockets();
                
                // 5. 게임 상태 초기화
                resetGameState();
                
                // 6. UI를 매칭 화면으로 복귀
                showMatchingScreen();
                
                console.log('✅ 게임 나가기 완료');
                
            } catch (error) {
                console.error('❌ 게임 나가기 오류:', error);
                alert('게임 나가기 중 오류가 발생했습니다: ' + error.message);
            }
        }
        
        // WebSocket 연결 모두 해제
        function disconnectAllWebSockets() {
            if (aiStompClient && aiStompClient.connected) {
                try {
                    aiStompClient.disconnect();
                    console.log('✅ AI WebSocket 연결 해제');
                } catch (error) {
                    console.warn('⚠️ AI WebSocket 해제 오류:', error);
                }
            }
            
            if (partyStompClient && partyStompClient.connected) {
                try {
                    partyStompClient.disconnect();
                    console.log('✅ 파티 WebSocket 연결 해제');
                } catch (error) {
                    console.warn('⚠️ 파티 WebSocket 해제 오류:', error);
                }
            }
            
            if (matchingStompClient && matchingStompClient.connected) {
                try {
                    matchingStompClient.disconnect();
                    console.log('✅ 매칭 WebSocket 연결 해제');
                } catch (error) {
                    console.warn('⚠️ 매칭 WebSocket 해제 오류:', error);
                }
            }
            
            aiStompClient = null;
            partyStompClient = null;
            matchingStompClient = null;
        }
        
        // 게임 상태 초기화 (로그아웃하지 않음)
        function resetGameState() {
            // 게임 관련 변수만 초기화
            gameSessionStarted = false;
            currentGameSession = null;
            aiGameRoomId = null;
            partyRoomId = null;
            isMatching = false;
            currentTurnNumber = 1;
            participantNicknames.clear(); // 참여자 목록 초기화
            
            // 타이머 정지
            resetTRPGTimer();
            
            // 채팅 비활성화
            disableChatInputs();
            
            // 채팅 메시지 초기화
            document.getElementById('aiMessages').innerHTML = '';
            document.getElementById('rightUserMessages').innerHTML = '';
        }
        
        // 매칭 화면으로 복귀
        function showMatchingScreen() {
            // UI 전환
            document.getElementById('gameSection').classList.add('hidden');
            document.getElementById('matchingSection').classList.remove('hidden');
            document.getElementById('leaveGameHeaderBtn').classList.add('hidden');
            
            // 상태 업데이트는 게임상태 창이 제거되어 더 이상 필요 없음
            console.log('매칭 화면으로 복귀');
            
            // 매칭 버튼 상태 초기화
            document.getElementById('startMatchingBtn').classList.remove('hidden');
            document.getElementById('cancelMatchingBtn').classList.add('hidden');
            document.getElementById('matchingStatus').classList.add('hidden');
            document.getElementById('startMatchingBtn').disabled = selectedWorld ? false : true;
            
            // 매칭 WebSocket 재연결
            setTimeout(() => {
                connectMatchingWebSocket();
            }, 1000);
        }
        
        
        // 채팅 입력 활성화
        function enableChatInputs() {
            document.getElementById('aiMessageInput').disabled = false;
            document.getElementById('aiSendBtn').disabled = false;
            document.getElementById('aiRequestBtn').disabled = false;
            document.getElementById('rightUserMessageInput').disabled = false;
            document.getElementById('rightUserSendBtn').disabled = false;
        }
        
        // 채팅 입력 비활성화
        function disableChatInputs() {
            document.getElementById('aiMessageInput').disabled = true;
            document.getElementById('aiSendBtn').disabled = true;
            document.getElementById('aiRequestBtn').disabled = true;
            document.getElementById('rightUserMessageInput').disabled = true;
            document.getElementById('rightUserSendBtn').disabled = true;
        }
        
        // 메시지 추가 함수들
        function addAiMessage(type, content) {
            const messagesDiv = document.getElementById('aiMessages');
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${type}`;
            
            // HTML 이스케이프 후 줄바꿈 및 포맷팅 처리
            const escapedContent = content
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#x27;');
                
            const formattedContent = escapedContent
                .replace(/\n/g, '<br>')           // \n을 <br>로 변환
                .replace(/\r\n/g, '<br>')        // \r\n을 <br>로 변환
                .replace(/\*{3,}/g, '<span style="color: #666; font-style: italic;">[BLOCKED]</span>')  // ***를 [BLOCKED]로 변환 (마크다운 처리 전에)
                .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **텍스트**를 굵게
                .replace(/\*(.*?)\*/g, '<em>$1</em>')            // *텍스트*를 기울임
                .replace(/`(.*?)`/g, '<code>$1</code>');         // `코드`를 코드 스타일
            
            messageDiv.innerHTML = formattedContent;
            messagesDiv.appendChild(messageDiv);
            
            // 사용자가 맨 아래 근처에 있을 때만 자동 스크롤 (스마트 스크롤)
            const isNearBottom = messagesDiv.scrollTop >= messagesDiv.scrollHeight - messagesDiv.clientHeight - 100;
            if (isNearBottom) {
                messagesDiv.scrollTop = messagesDiv.scrollHeight;
            }
        }
        
        
        function addRightUserMessage(type, content) {
            const messagesDiv = document.getElementById('rightUserMessages');
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${type}`;
            
            // HTML 이스케이프 후 줄바꿈 및 포맷팅 처리
            const escapedContent = content
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#x27;');
                
            const formattedContent = escapedContent
                .replace(/\n/g, '<br>')           // \n을 <br>로 변환
                .replace(/\r\n/g, '<br>')        // \r\n을 <br>로 변환
                .replace(/\*{3,}/g, '<span style="color: #666; font-style: italic;">[BLOCKED]</span>')  // ***를 [BLOCKED]로 변환 (마크다운 처리 전에)
                .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **텍스트**를 굵게
                .replace(/\*(.*?)\*/g, '<em>$1</em>')            // *텍스트*를 기울임
                .replace(/`(.*?)`/g, '<code>$1</code>');         // `코드`를 코드 스타일
            
            messageDiv.innerHTML = formattedContent;
            messagesDiv.appendChild(messageDiv);
            
            // 사용자가 맨 아래 근처에 있을 때만 자동 스크롤 (스마트 스크롤)
            const isNearBottom = messagesDiv.scrollTop >= messagesDiv.scrollHeight - messagesDiv.clientHeight - 100;
            if (isNearBottom) {
                messagesDiv.scrollTop = messagesDiv.scrollHeight;
            }
        }
        
        // 참여자 목록 업데이트 함수 제거됨
        
        
        // 유틸리티 함수들
        function updateStatus(elementId, value) {
            const element = document.getElementById(elementId);
            if (element) {
                element.textContent = value;
            }
        }
        
        function getCurrentTurnNumber() {
            return currentTurnNumber;
        }
        
        function incrementTurnNumber() {
            currentTurnNumber++;
        }
        
        // JWT 토큰 디코딩 함수
        function decodeJWT(token) {
            try {
                // JWT는 3부분으로 나뉘어지는데, 두 번째 부분이 payload
                const base64Url = token.split('.')[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
                
                return JSON.parse(jsonPayload);
            } catch (error) {
                console.error('JWT 디코딩 오류:', error);
                return null;
            }
        }
        
        // JWT에서 사용자 정보 추출
        function extractUserInfoFromToken(token) {
            const payload = decodeJWT(token);
            if (payload) {
                return {
                    id: payload.id || payload.sub, // subject 또는 id 필드 사용
                    name: payload.name,
                    nickname: payload.nickName
                };
            }
            return null;
        }
        
        // 세계관별 환영 메시지
        function addWorldSpecificWelcomeMessages(worldType) {
            switch(worldType) {
                case 'FANTASY':
                    addAiMessage('system', '🌟 중세 판타지 세계에 오신 것을 환영합니다!');
                    addAiMessage('system', '⚔️ 마법과 검의 세계에서 전설적인 모험을 시작하세요!');
                    addAiMessage('ai', '안녕하세요, 용감한 모험가들이여! 저는 여러분의 운명을 안내하는 고대의 지식을 가진 AI 오라클입니다. 이 신비로운 대륙에서 여러분의 영웅적인 여정이 시작됩니다. 먼저 자신을 소개하고, 어떤 모험을 찾고 있는지 말해주세요.');
                    break;
                case 'ZOMBIE':
                    addAiMessage('system', '🧟 좀비 아포칼립스 세계에 오신 것을 환영합니다!');
                    addAiMessage('system', '🔫 생존이 최우선인 위험한 세계입니다!');
                    addAiMessage('ai', '안녕하세요, 생존자들이여. 저는 이 황폐한 세계에서 여러분의 생존을 도울 AI 가이드입니다. 바이러스로 인해 세상이 바뀐 지 이제 몇 개월이 흘렀습니다. 먼저 각자 자신이 누구였는지, 그리고 어떻게 여기까지 살아남았는지 말해주세요.');
                    break;
                default:
                    addAiMessage('system', `🌟 ${getWorldName(selectedWorld)} 세계에 오신 것을 환영합니다!`);
                    addAiMessage('system', '🤖 AI 마스터가 여러분의 모험을 안내할 것입니다.');
                    addAiMessage('ai', `안녕하세요 모험가들! 저는 여러분의 게임 마스터 AI입니다. ${getWorldName(selectedWorld)} 세계에서 펼쳐진 모험을 함께 만들어갑시다. 먼저 자기소개를 해주세요!`);
            }
        }

        // =============================================
        // 15분 TRPG 타이머 시스템
        // =============================================

        /**
         * TRPG 게임 타이머 시작
         */
        function startTRPGTimer() {
            if (gameStartTime) return; // 이미 시작됨
            
            gameStartTime = Date.now();
            console.log('🕐 TRPG 15분 타이머 시작!');
            
            // 1초마다 타이머 업데이트
            timerInterval = setInterval(updateTRPGTimer, 1000);
            
            // 첫 업데이트
            updateTRPGTimer();
        }

        /**
         * 타이머 업데이트
         */
        function updateTRPGTimer() {
            if (!gameStartTime) return;
            
            const elapsed = (Date.now() - gameStartTime) / 1000; // 초 단위
            const elapsedMinutes = elapsed / 60;
            const remaining = Math.max(0, (targetGameDuration * 60) - elapsed);
            const remainingMinutes = Math.floor(remaining / 60);
            const remainingSeconds = Math.floor(remaining % 60);
            
            // 타이머 텍스트 업데이트
            const timerText = document.querySelector('.timer-text');
            if (timerText) {
                timerText.textContent = `${remainingMinutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
            }
            
            // 프로그레스 바 업데이트
            const progress = document.querySelector('.timer-progress');
            if (progress) {
                const percentage = Math.max(0, (remaining / (targetGameDuration * 60)) * 100);
                progress.style.width = `${percentage}%`;
            }
            
            // 게임 단계 계산 및 업데이트
            updateGamePhase(elapsedMinutes, remaining);
            
            // 시간 종료 체크
            if (remaining <= 0) {
                handleGameTimeEnd();
            }
        }

        /**
         * 게임 단계 업데이트
         */
        function updateGamePhase(elapsedMinutes, remaining) {
            let newPhase = currentGamePhase;
            
            if (remaining <= 0) {
                newPhase = 'ending';
            } else if (remaining <= 3 * 60) { // 3분 이하
                newPhase = 'climax';
            } else if (remaining <= 7 * 60) { // 7분 이하
                newPhase = 'middle';
            } else if (elapsedMinutes >= 2) { // 2분 경과
                newPhase = 'development';
            } else if (elapsedMinutes > 0) {
                newPhase = 'intro';
            }
            
            if (newPhase !== currentGamePhase) {
                currentGamePhase = newPhase;
                updatePhaseUI();
                console.log(`🎭 게임 단계 변경: ${gamePhasesData[currentGamePhase].name}`);
            }
        }

        /**
         * 게임 단계 UI 업데이트
         */
        function updatePhaseUI() {
            const phaseData = gamePhasesData[currentGamePhase];
            
            // 단계 텍스트 업데이트
            const phaseText = document.querySelector('.phase-text');
            if (phaseText) {
                phaseText.textContent = phaseData.name;
            }
            
            // 압박도 텍스트 및 스타일 업데이트
            const pressureText = document.querySelector('.pressure-text');
            if (pressureText) {
                const pressureNames = {
                    relaxed: '여유',
                    normal: '보통',
                    urgent: '급박',
                    critical: '매우 급박'
                };
                
                pressureText.textContent = pressureNames[phaseData.pressure];
                pressureText.className = `pressure-text pressure-${phaseData.pressure}`;
            }
        }

        /**
         * 게임 시간 종료 처리
         */
        function handleGameTimeEnd() {
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }
            
            console.log('⏰ TRPG 15분 시간 종료!');
            
            // 종료 메시지 추가
            addAiMessage('system', '⏰ 15분 게임 시간이 종료되었습니다!');
            addAiMessage('system', '🎭 게임 마스터가 마무리 중...');
            
            // UI 업데이트
            const timerText = document.querySelector('.timer-text');
            if (timerText) {
                timerText.textContent = '00:00';
                timerText.style.color = '#F44336';
            }
        }

        /**
         * 타이머 초기화
         */
        function resetTRPGTimer() {
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }
            
            gameStartTime = null;
            currentGamePhase = 'waiting';
            
            const timerText = document.querySelector('.timer-text');
            if (timerText) {
                timerText.textContent = '15:00';
                timerText.style.color = '#333';
            }
            
            const progress = document.querySelector('.timer-progress');
            if (progress) {
                progress.style.width = '100%';
            }
            
            updatePhaseUI();
            console.log('🔄 TRPG 타이머 초기화');
        }

        // 게임 시작 시 타이머 자동 시작 (기존 함수 수정 필요)
        const originalStartGameSession = startGameSession;
        startGameSession = function(sessionData) {
            originalStartGameSession.call(this, sessionData);
            // 게임 세션 시작 시 타이머도 시작
            setTimeout(() => {
                if (gameSessionStarted && !gameStartTime) {
                    startTRPGTimer();
                }
            }, 2000); // 2초 후 타이머 시작
        };
        
        // ============ 캐릭터 생성 관련 함수들 ============
        
        // 캐릭터 존재 여부 체크
        async function checkCharacterExists(memberId) {
            try {
                const response = await fetch(`/v1/characters/exists?memberId=${memberId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    }
                });
                
                if (!response.ok) {
                    throw new Error('캐릭터 존재 여부 확인 실패: ' + response.status);
                }
                
                const data = await response.json();
                console.log('캐릭터 존재 여부 체크 결과:', data);
                
                return data.data; // boolean 값 반환
            } catch (error) {
                console.error('캐릭터 존재 여부 체크 오류:', error);
                throw error;
            }
        }
        
        // 캐릭터 생성 모달 표시
        async function showCharacterCreationModal() {
            try {
                // 종족 목록 로드
                await loadRaces();
                
                // 모달 표시
                document.getElementById('characterCreationModal').style.display = 'flex';
            } catch (error) {
                console.error('캐릭터 생성 모달 표시 오류:', error);
                alert('종족 정보를 불러오는데 실패했습니다.');
            }
        }
        
        // 캐릭터 생성 모달 숨기기
        function hideCharacterCreationModal() {
            document.getElementById('characterCreationModal').style.display = 'none';
            document.getElementById('raceSelect').value = '';
            document.getElementById('raceDescription').style.display = 'none';
            document.getElementById('createCharacterBtn').disabled = true;
        }
        
        // 종족 목록 로드
        async function loadRaces() {
            try {
                const response = await fetch('/v1/characters/races', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    }
                });
                
                if (!response.ok) {
                    throw new Error('종족 목록 조회 실패: ' + response.status);
                }
                
                const data = await response.json();
                console.log('종족 목록 조회 성공:', data);
                
                const raceSelect = document.getElementById('raceSelect');
                
                // 기존 옵션 제거 (첫 번째 옵션 제외)
                while (raceSelect.children.length > 1) {
                    raceSelect.removeChild(raceSelect.lastChild);
                }
                
                // 종족 옵션 추가
                if (data.data && Array.isArray(data.data)) {
                    data.data.forEach(raceName => {
                        const option = document.createElement('option');
                        option.value = raceName;
                        option.textContent = raceName;
                        raceSelect.appendChild(option);
                    });
                    
                    // 종족 선택 이벤트 리스너 추가
                    raceSelect.addEventListener('change', function() {
                        const selectedRace = this.value;
                        if (selectedRace) {
                            showRaceDescription(selectedRace);
                            document.getElementById('createCharacterBtn').disabled = false;
                        } else {
                            document.getElementById('raceDescription').style.display = 'none';
                            document.getElementById('createCharacterBtn').disabled = true;
                        }
                    });
                } else {
                    throw new Error('종족 데이터 형식이 올바르지 않습니다.');
                }
                
            } catch (error) {
                console.error('종족 목록 로드 오류:', error);
                throw error;
            }
        }
        
        // 종족 설명 표시
        function showRaceDescription(raceName) {
            const descriptions = {
                '엘프': {
                    title: '🧝‍♂️ 엘프',
                    content: '마법에 뛰어난 우아한 종족입니다. 높은 마나와 마법 공격력을 가지고 있으며, 회피율도 뛰어납니다. 하지만 물리적인 체력은 상대적으로 낮습니다.'
                },
                '인간': {
                    title: '👨‍🦰 인간',
                    content: '균형잡힌 능력을 가진 다재다능한 종족입니다. 모든 능력치가 고르게 분포되어 있어 다양한 전투 상황에 적응할 수 있습니다.'
                },
                '드워프': {
                    title: '🧔 드워프',
                    content: '강인한 체력과 강력한 물리 공격력을 자랑하는 전사 종족입니다. 높은 HP와 물리 공격력을 가지고 있지만, 마법 능력과 회피율은 낮습니다.'
                }
            };
            
            const desc = descriptions[raceName];
            if (desc) {
                document.getElementById('raceDescTitle').textContent = desc.title;
                document.getElementById('raceDescContent').textContent = desc.content;
                document.getElementById('raceDescription').style.display = 'block';
            }
        }
        
        // 캐릭터 생성
        async function createCharacter() {
            const selectedRace = document.getElementById('raceSelect').value;
            if (!selectedRace) {
                alert('종족을 선택해주세요.');
                return;
            }
            
            try {
                console.log('캐릭터 생성 API 호출:', { memberId: currentUser.id, raceId: selectedRace });
                
                const response = await fetch('/v1/characters', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    },
                    body: JSON.stringify({
                        memberId: currentUser.id,
                        raceId: selectedRace
                    })
                });
                
                if (!response.ok) {
                    throw new Error('캐릭터 생성 실패: ' + response.status);
                }
                
                const data = await response.json();
                console.log('캐릭터 생성 성공:', data);
                
                // 모달 숨기기
                hideCharacterCreationModal();
                
                // 성공 메시지
                alert(`${selectedRace} 캐릭터가 생성되었습니다! 이제 세계관을 선택하고 게임을 시작할 수 있습니다.`);
                
            } catch (error) {
                console.error('캐릭터 생성 오류:', error);
                alert('캐릭터 생성 중 오류가 발생했습니다: ' + error.message);
            }
        }
        
        // ============ 내 캐릭터 모달 관련 함수들 ============
        
        // 내 캐릭터 모달 표시
        async function showMyCharacterModal() {
            try {
                const hasCharacter = await checkCharacterExists(currentUser.id);
                
                if (hasCharacter) {
                    // 캐릭터 상세 정보 로드
                    await loadMyCharacterDetails();
                    document.getElementById('noCharacterSection').style.display = 'none';
                    document.getElementById('hasCharacterSection').style.display = 'block';
                } else {
                    // 캐릭터 없음 섹션 표시
                    document.getElementById('hasCharacterSection').style.display = 'none';
                    document.getElementById('noCharacterSection').style.display = 'block';
                }
                
                document.getElementById('myCharacterModal').style.display = 'flex';
            } catch (error) {
                console.error('내 캐릭터 모달 표시 오류:', error);
                alert('캐릭터 정보를 불러오는데 실패했습니다.');
            }
        }
        
        // 내 캐릭터 모달 숨기기
        function hideMyCharacterModal() {
            document.getElementById('myCharacterModal').style.display = 'none';
        }
        
        // 내 캐릭터 상세 정보 로드
        async function loadMyCharacterDetails() {
            try {
                // 기본 캐릭터 정보 조회
                const basicResponse = await fetch(`/v1/characters?memberId=${currentUser.id}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    }
                });
                
                if (!basicResponse.ok) {
                    throw new Error('캐릭터 기본 정보 조회 실패: ' + basicResponse.status);
                }
                
                const basicData = await basicResponse.json();
                const character = basicData.data;
                
                // 캐릭터 상세 정보 조회 (계산된 스탯 포함)
                const detailResponse = await fetch(`/v1/characters/${character.id}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + authToken,
                        'Cache-Control': 'no-cache'
                    }
                });
                
                if (!detailResponse.ok) {
                    throw new Error('캐릭터 상세 정보 조회 실패: ' + detailResponse.status);
                }
                
                const detailData = await detailResponse.json();
                const characterDetail = detailData.data;
                
                // UI 업데이트
                updateCharacterUI(character, characterDetail);
                
            } catch (error) {
                console.error('캐릭터 상세 정보 로드 오류:', error);
                throw error;
            }
        }
        
        // 캐릭터 UI 업데이트
        function updateCharacterUI(basicInfo, detailInfo) {
            // 기본 정보
            document.getElementById('charRace').textContent = detailInfo.raceName || basicInfo.raceId;
            document.getElementById('charLevel').textContent = basicInfo.playerLevel;
            document.getElementById('charExp').textContent = basicInfo.totalExp.toLocaleString();
            document.getElementById('charUnspentPoints').textContent = basicInfo.unspentPoints;
            
            // 기본 스탯
            document.getElementById('charStrength').textContent = basicInfo.strength;
            document.getElementById('charWillpower').textContent = basicInfo.willpower;
            document.getElementById('charIntelligence').textContent = basicInfo.intelligence;
            document.getElementById('charWisdom').textContent = basicInfo.wisdom;
            document.getElementById('charDexterity').textContent = basicInfo.dexterity;
            document.getElementById('charLuck').textContent = basicInfo.luck;
            
            // 계산된 스탯
            const calculatedStatsContainer = document.getElementById('calculatedStats');
            calculatedStatsContainer.innerHTML = '';
            
            if (detailInfo.calculatedStats) {
                const statDisplayNames = {
                    'hp': 'HP',
                    'mp': 'MP',
                    'physicalAttack': '물리 공격력',
                    'magicAttack': '마법 공격력',
                    'evasionRate': '회피율',
                    'accuracy': '명중률',
                    'diceOdds': '주사위 확률'
                };
                
                Object.entries(detailInfo.calculatedStats).forEach(([statKey, value]) => {
                    const displayName = statDisplayNames[statKey] || statKey;
                    const statElement = document.createElement('div');
                    statElement.className = 'calculated-stat-item';
                    statElement.innerHTML = `
                        <div class="stat-name">${displayName}</div>
                        <div class="stat-value">${typeof value === 'number' ? value.toFixed(1) : value}</div>
                    `;
                    calculatedStatsContainer.appendChild(statElement);
                });
            }
        }
        
        // 종족 변경
        async function changeCharacterRace() {
            const confirmed = confirm('정말로 종족을 변경하시겠습니까?\n기존 캐릭터가 삭제되고 새로운 캐릭터가 생성됩니다.');
            if (!confirmed) return;
            
            try {
                // 기존 캐릭터 삭제는 새 캐릭터 생성으로 자동 처리됨
                hideMyCharacterModal();
                await showCharacterCreationModal();
            } catch (error) {
                console.error('종족 변경 오류:', error);
                alert('종족 변경 중 오류가 발생했습니다.');
            }
        }
        
        
        // 로그아웃 함수 개선
        function logout() {
            const confirmed = confirm('정말로 로그아웃하시겠습니까?');
            if (!confirmed) return;
            
            clearAuthData();
            document.getElementById('headerActions').style.display = 'none';
            document.getElementById('loginSection').style.display = 'block';
            document.getElementById('loggedInSection').style.display = 'none';
            
            // 모달들 닫기
            hideMyCharacterModal();
            hideCharacterCreationModal();
        }
        
