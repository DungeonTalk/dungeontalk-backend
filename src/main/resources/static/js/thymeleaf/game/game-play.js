// Game Play Module - 실제 게임 진행 로직
function gamePlayApp() {
    return {
        // 이전 페이지에서 받은 데이터
        selectedWorld: null,
        authToken: null,
        currentUser: null,
        
        // Game Status
        gameStatus: '매칭 대기',
        matchingInfo: '매칭 준비',
        roomInfo: '미연결',
        gameStarted: false,
        
        // Matching
        isMatching: true,
        matchingStatusText: '매칭을 찾는 중...',
        queueInfo: '',
        
        // WebSocket Clients
        matchingStompClient: null,
        aiStompClient: null,
        partyStompClient: null,
        
        // Game Session
        currentGameSession: null,
        currentTurnNumber: 1,
        aiGameRoomId: null,
        partyRoomId: null,
        
        // Chat
        activeTab: 'ai',
        aiInput: '',
        playerInput: '',
        aiMessages: [],
        playerMessages: [],
        
        // Participants
        participants: [],
        
        // Timer & Game Phase
        gameTime: 900,
        gameInterval: null,
        gamePhase: 'relaxed',
        gameEndReason: null,
        
        // Character System
        showCharacterInfo: false,
        characterData: null,
        
        init() {
            // URL 파라미터에서 세계관 정보 가져오기
            const urlParams = new URLSearchParams(window.location.search);
            const worldId = urlParams.get('world');
            
            if (!worldId) {
                Alpine.store('notifications').error('세계관 정보가 없습니다.');
                setTimeout(() => window.location.href = '/game', 1000);
                return;
            }
            
            // sessionStorage에서 세계관 정보 가져오기
            const worldData = sessionStorage.getItem('selectedWorld');
            if (worldData) {
                const world = JSON.parse(worldData);
                this.selectedWorld = world;
            } else {
                this.selectedWorld = { id: worldId, name: worldId };
            }
            
            this.checkAuth();
            
            // 자동으로 매칭 시작
            setTimeout(() => this.startMatching(), 1000);
            
            // Handle page unload
            window.addEventListener('beforeunload', () => this.cleanup());
        },
        
        // Timer Functions (동일한 타이머 함수들)
        formatTime(seconds) {
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        },
        
        updateGamePhase() {
            if (this.gameTime >= 600) {
                this.gamePhase = 'relaxed';
            } else if (this.gameTime >= 300) {
                this.gamePhase = 'normal';
            } else if (this.gameTime >= 60) {
                this.gamePhase = 'urgent';
            } else {
                this.gamePhase = 'critical';
            }
        },
        
        getPhaseText(phase) {
            const texts = {
                'relaxed': '🌅 도입',
                'normal': '⚡ 전개',
                'urgent': '🔥 클라이맥스',
                'critical': '🎭 종료'
            };
            return texts[phase] || '준비 중';
        },
        
        getGamePhaseEmoji(phase) {
            const emojis = {
                'relaxed': '🌱',
                'normal': '⚡',
                'urgent': '🔥',
                'critical': '💥'
            };
            return emojis[phase] || '⏳';
        },
        
        getPressureText(phase) {
            const texts = {
                'relaxed': '여유',
                'normal': '보통',
                'urgent': '긴급',
                'critical': '위급'
            };
            return texts[phase] || '대기';
        },
        
        getPressureEmoji(phase) {
            const emojis = {
                'relaxed': '😌',
                'normal': '🤔',
                'urgent': '😰',
                'critical': '😱'
            };
            return emojis[phase] || '😐';
        },
        
        getPressureMessage(phase) {
            const messages = {
                'relaxed': '여유롭게 탐험하세요',
                'normal': '진행 속도를 높이세요',
                'urgent': '서둘러 임무를 완수하세요!',
                'critical': '마지막 기회입니다!'
            };
            return messages[phase] || '게임 진행 중';
        },
        
        startGameTimer() {
            this.gameTime = 900;
            this.updateGamePhase();
            
            if (this.gameInterval) {
                clearInterval(this.gameInterval);
            }
            
            this.gameInterval = setInterval(() => {
                this.gameTime--;
                this.updateGamePhase();
                
                if (this.gameTime <= 0) {
                    clearInterval(this.gameInterval);
                    this.gameInterval = null;
                    this.handleGameEnd('timeout');
                }
                
                // 압박감 알림 추가
                if (this.gameTime === 600) {
                    Alpine.store('notifications').info('⏰ 10분 남았습니다. 본격적인 모험을 시작하세요!');
                    this.addSystemMessage('ai', '⚡ 게임이 전개 단계로 진입했습니다.');
                } else if (this.gameTime === 300) {
                    Alpine.store('notifications').warning('🔥 5분 남았습니다! 클라이맥스에 도달했습니다!');
                    this.addSystemMessage('ai', '🔥 긴장감이 최고조에 달했습니다! 서둘러 임무를 완수하세요!');
                    // 화면 효과 추가
                    this.triggerPressureEffect('urgent');
                } else if (this.gameTime === 120) {
                    Alpine.store('notifications').warning('⚠️ 2분 남았습니다! 결말을 준비하세요!');
                    this.addSystemMessage('ai', '⚠️ 시간이 얼마 남지 않았습니다!');
                } else if (this.gameTime === 60) {
                    Alpine.store('notifications').error('💥 1분 남았습니다! 마지막 기회입니다!');
                    this.addSystemMessage('ai', '💥 최후의 순간입니다! 운명을 결정하세요!');
                    // 강한 화면 효과
                    this.triggerPressureEffect('critical');
                } else if (this.gameTime === 30) {
                    Alpine.store('notifications').error('🚨 30초! 마지막 선택을 하세요!');
                    this.addSystemMessage('ai', '🚨 30초 남았습니다!');
                } else if (this.gameTime === 10) {
                    // 카운트다운 시작
                    this.startFinalCountdown();
                }
            }, 1000);
        },
        
        stopGameTimer() {
            if (this.gameInterval) {
                clearInterval(this.gameInterval);
                this.gameInterval = null;
            }
        },
        
        checkAuth() {
            // Auth 체크 로직 (game-app.js와 동일)
            this.authToken = localStorage.getItem('authToken');
            let userInfo = localStorage.getItem('currentUser');
            
            if (!this.authToken) {
                const cookies = document.cookie.split(';');
                for (let cookie of cookies) {
                    const [name, value] = cookie.trim().split('=');
                    if (name === 'accessToken') {
                        this.authToken = value;
                    } else if (name === 'userInfo' && !userInfo) {
                        userInfo = decodeURIComponent(value);
                        localStorage.setItem('currentUser', userInfo);
                    }
                }
            }
            
            if (!this.authToken) {
                Alpine.store('notifications').error('로그인이 필요합니다.');
                setTimeout(() => window.location.href = '/login', 2000);
                return;
            }
            
            if (!userInfo) {
                userInfo = JSON.stringify({
                    id: 'user_' + Date.now(),
                    nickname: '플레이어'
                });
                localStorage.setItem('currentUser', userInfo);
            }
            
            try {
                this.currentUser = JSON.parse(userInfo);
                this.gameStatus = '매칭 준비';
            } catch (e) {
                console.error('사용자 정보 파싱 오류:', e);
                localStorage.clear();
                window.location.href = '/login';
            }
        },
        
        async startMatching() {
            this.isMatching = true;
            this.matchingInfo = '매칭 시작';
            
            try {
                await this.connectMatchingWebSocket();
                
                const message = {
                    type: 'MATCH_REQUEST',
                    userId: this.currentUser.id,
                    userName: this.currentUser.nickname || this.currentUser.id,
                    worldType: this.selectedWorld.id
                };
                
                this.matchingStompClient.send('/pub/matching/request', {}, JSON.stringify(message));
                Alpine.store('notifications').info('매칭을 시작합니다...');
                
            } catch (error) {
                console.error('매칭 시작 오류:', error);
                Alpine.store('notifications').error('매칭 시작에 실패했습니다.');
                this.isMatching = false;
            }
        },
        
        async connectMatchingWebSocket() {
            return new Promise((resolve, reject) => {
                const socket = new SockJS('/ws-matching');
                this.matchingStompClient = Stomp.over(socket);
                
                this.matchingStompClient.connect(
                    {},  // 쿠키 기반 인증 사용
                    () => {
                        this.matchingStompClient.subscribe(
                            `/user/${this.currentUser.id}/queue/matching`,
                            (message) => this.handleMatchingMessage(JSON.parse(message.body))
                        );
                        
                        console.log('매칭 WebSocket 연결 성공');
                        resolve();
                    },
                    (error) => {
                        console.error('매칭 WebSocket 연결 실패:', error);
                        reject(error);
                    }
                );
            });
        },
        
        handleMatchingMessage(message) {
            console.log('매칭 메시지:', message);
            
            switch (message.type) {
                case 'QUEUE_STATUS_UPDATE':  // dungeon-game.html의 큐 상태 업데이트 처리
                    this.updateQueueStatus(message.data);
                    break;
                    
                case 'MATCH_WAITING':
                    this.matchingStatusText = '매칭을 찾는 중...';
                    this.queueInfo = `대기 중인 플레이어: ${message.queueSize || 0}명`;
                    break;
                    
                case 'MATCH_FOUND':
                    this.matchingStatusText = '매칭 찾기 완료! 게임을 준비 중...';
                    Alpine.store('notifications').success('매칭이 완료되었습니다!');
                    break;
                    
                case 'MATCHING_COMPLETE':  // dungeon-game.html의 매칭 완료 처리
                    this.handleMatchingComplete(message.data);
                    break;
                    
                case 'GAME_READY':
                    this.isMatching = false;
                    this.currentGameSession = message;
                    this.participants = message.players.map(p => ({
                        id: p.userId,
                        nickname: p.userName,
                        isMe: p.userId === this.currentUser.id
                    }));
                    this.startGame(message);
                    break;
                    
                case 'MATCHING_CANCELLED':  // dungeon-game.html의 매칭 취소 처리
                    this.handleMatchingCancelled();
                    break;
                    
                case 'MATCH_FAILED':
                    this.isMatching = false;
                    this.matchingInfo = '매칭 실패';
                    Alpine.store('notifications').error(message.reason || '매칭에 실패했습니다.');
                    setTimeout(() => window.location.href = '/game', 3000);
                    break;
                    
                case 'ERROR':  // dungeon-game.html의 에러 처리
                    console.error('매칭 오류:', message.data);
                    Alpine.store('notifications').error('매칭 오류: ' + message.data);
                    break;
            }
        },
        
        // 큐 상태 업데이트 (dungeon-game.html에서 가져온 기능)
        updateQueueStatus(data) {
            if (!data) return;
            
            const queueText = [];
            
            if (data.currentPosition !== undefined) {
                queueText.push(`대기 순서: ${data.currentPosition}번째`);
            }
            
            if (data.totalInQueue !== undefined) {
                queueText.push(`총 대기자: ${data.totalInQueue}명`);
            }
            
            if (data.estimatedMessage) {
                queueText.push(`예상 시간: ${data.estimatedMessage}`);
            } else if (data.estimatedWaitTime !== undefined) {
                const minutes = Math.floor(data.estimatedWaitTime / 60);
                const seconds = data.estimatedWaitTime % 60;
                queueText.push(`예상 시간: ${minutes}분 ${seconds}초`);
            }
            
            this.queueInfo = queueText.join(' | ');
            this.matchingStatusText = '매칭 대기 중...';
            
            // 대기 순서에 따른 상태 메시지 업데이트
            if (data.currentPosition === 1) {
                this.matchingStatusText = '곧 매칭이 시작됩니다!';
            } else if (data.currentPosition <= 3) {
                this.matchingStatusText = '매칭이 임박했습니다...';
            }
        },
        
        // 매칭 완료 처리 (dungeon-game.html에서 가져온 기능)
        handleMatchingComplete(data) {
            console.log('매칭 완료 데이터:', data);
            
            this.isMatching = false;
            this.currentGameSession = data;
            
            // 참여자 정보 설정
            if (data.participants) {
                this.participants = data.participants.map(p => ({
                    id: p.id || p.userId,
                    nickname: p.nickname || p.userName,
                    isMe: (p.id || p.userId) === this.currentUser.id
                }));
            }
            
            // 게임 시작
            this.startGame(data);
        },
        
        // 매칭 취소 처리
        handleMatchingCancelled() {
            this.isMatching = false;
            this.matchingStatusText = '매칭이 취소되었습니다.';
            this.queueInfo = '';
            Alpine.store('notifications').info('매칭이 취소되었습니다.');
            setTimeout(() => window.location.href = '/game', 2000);
        },
        
        async startGame(gameSession) {
            this.gameStarted = true;
            this.gameStatus = '게임 진행 중';
            this.roomInfo = `게임 ID: ${gameSession.sessionId}`;
            
            this.startGameTimer();
            
            await this.createAiGameRoom(gameSession);
            await this.connectAiWebSocket();
            await this.connectPartyWebSocket(gameSession.chatRoomId);
            
            const worldGreeting = this.getWorldGreeting(this.selectedWorld.id);
            this.addAiMessage('system', `🎮 게임이 시작되었습니다! 15분 동안의 모험이 시작됩니다.`);
            this.addAiMessage('ai', worldGreeting);
            this.addPlayerMessage('system', '플레이어 채팅방이 연결되었습니다.');
            
            Alpine.store('notifications').success('게임이 시작되었습니다!');
        },
        
        getWorldGreeting(worldType) {
            const greetings = {
                'FANTASY': '⚔️ 용사여, 고대의 던전에 오신 것을 환영합니다! 전설의 보물을 찾기 위한 당신의 모험이 지금 시작됩니다. 주변을 둘러보시겠습니까?',
                'SCIFI': '🚀 에이전트, 버려진 우주 정거장 알파-7에 도착했습니다. 이곳에서 실종된 연구팀의 흔적을 찾아야 합니다. 스캐너를 작동시키시겠습니까?',
                'MODERN': '🕵️ 탐정님, 도시의 미스터리한 사건 현장에 도착했습니다. 단서를 찾아 진실을 밝혀내야 합니다. 현장을 조사하시겠습니까?'
            };
            return greetings[worldType] || '모험이 시작되었습니다. 무엇을 하시겠습니까?';
        },
        
        async handleGameEnd(reason) {
            this.gameEndReason = reason;
            this.stopGameTimer();
            
            let endMessage = '';
            let isSuccess = false;
            
            switch(reason) {
                case 'timeout':
                    endMessage = '⏰ 시간이 초과되었습니다! 15분 동안의 모험이 끝났습니다.';
                    break;
                case 'victory':
                case 'SUCCESS':
                    endMessage = '🎉 축하합니다! 임무를 성공적으로 완수했습니다!';
                    isSuccess = true;
                    break;
                case 'defeat':
                case 'FAILURE':
                    endMessage = '💀 안타깝습니다. 임무에 실패했습니다.';
                    break;
                case 'leave':
                    endMessage = '👋 게임을 나갔습니다.';
                    break;
                case 'TIMEOUT':
                    endMessage = '⏰ 시간이 초과되어 게임이 종료되었습니다!';
                    break;
                default:
                    endMessage = '🎭 게임이 종료되었습니다.';
            }
            
            // 게임 종료 배너 표시
            this.showGameEndBanner(reason, endMessage);
            
            this.addAiMessage('system', endMessage);
            this.addPlayerMessage('system', endMessage);
            
            // 입력 필드 비활성화
            this.disableChatInputs();
            
            if (this.aiGameRoomId && reason !== 'leave') {
                try {
                    await fetch(`/v1/ai-game-room/${this.aiGameRoomId}/end`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        credentials: 'include',  // 쿠키 포함
                        body: JSON.stringify({
                            endReason: reason,
                            success: isSuccess,
                            finalTurn: this.currentTurnNumber
                        })
                    });
                } catch (error) {
                    console.error('게임 종료 처리 오류:', error);
                }
            }
            
            this.gameStatus = '🔴 게임 종료';
            this.roomInfo = '🎯 게임 완료';
            Alpine.store('notifications').info(endMessage);
            
            setTimeout(() => {
                window.location.href = '/game';
            }, 5000);
        },
        
        // [GAME_END] 키워드로 게임 종료 처리
        handleGameEndWithResult(gameResult, message) {
            console.log('🎮 게임 종료:', gameResult, message);
            this.handleGameEnd(gameResult);
        },
        
        // 게임 결과 분석 (dungeon-game.html에서 가져온 기능)
        analyzeGameResult(content) {
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
        },
        
        // 게임 종료 배너 표시
        showGameEndBanner(gameResult, message) {
            // 기존 배너가 있으면 제거
            const existingBanner = document.getElementById('gameEndBanner');
            if (existingBanner) {
                existingBanner.remove();
            }
            
            // 배너 생성
            const banner = document.createElement('div');
            banner.id = 'gameEndBanner';
            banner.className = 'fixed top-0 left-0 right-0 z-50 p-6 text-center text-white font-bold text-2xl shadow-lg';
            
            // 결과에 따른 스타일
            switch(gameResult) {
                case 'victory':
                case 'SUCCESS':
                    banner.className += ' bg-gradient-to-r from-green-500 to-emerald-500';
                    banner.innerHTML = '🎆 축하합니다! 임무 성공! 🎆';
                    break;
                case 'defeat':
                case 'FAILURE':
                    banner.className += ' bg-gradient-to-r from-red-500 to-pink-500';
                    banner.innerHTML = '💀 게임 오버! 다시 도전하세요! 💀';
                    break;
                case 'TIMEOUT':
                case 'timeout':
                    banner.className += ' bg-gradient-to-r from-orange-500 to-yellow-500';
                    banner.innerHTML = '⏰ 시간 초과! 게임이 종료되었습니다! ⏰';
                    break;
                default:
                    banner.className += ' bg-gradient-to-r from-gray-500 to-slate-500';
                    banner.innerHTML = '🎭 게임이 종료되었습니다! 🎭';
            }
            
            // 배너를 페이지 상단에 추가
            document.body.insertBefore(banner, document.body.firstChild);
            
            // 5초 후 배너 자동 제거
            setTimeout(() => {
                if (banner.parentNode) {
                    banner.style.transition = 'opacity 0.5s';
                    banner.style.opacity = '0';
                    setTimeout(() => banner.remove(), 500);
                }
            }, 5000);
        },
        
        // 채팅 입력 비활성화
        disableChatInputs() {
            // Alpine.js로 상태 변경
            this.gameStarted = false;
            
            // DOM 요소 직접 비활성화 (폴백)
            const inputs = document.querySelectorAll('input[type="text"], button');
            inputs.forEach(el => {
                if (el.id && (el.id.includes('Input') || el.id.includes('Btn'))) {
                    el.disabled = true;
                }
            });
        },
        
        // WebSocket 연결 및 메시지 처리 함수들...
        async createAiGameRoom(gameSession) {
            try {
                const response = await fetch('/v1/ai-game-room', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include',  // 쿠키 포함
                    body: JSON.stringify({
                        sessionId: gameSession.sessionId,
                        worldType: this.selectedWorld.id,
                        players: gameSession.players
                    })
                });
                
                if (!response.ok) throw new Error('AI 게임방 생성 실패');
                
                const result = await response.json();
                this.aiGameRoomId = result.data.roomId;
                console.log('AI 게임방 생성:', this.aiGameRoomId);
                
            } catch (error) {
                console.error('AI 게임방 생성 오류:', error);
                Alpine.store('notifications').error('AI 게임방 생성에 실패했습니다.');
            }
        },
        
        async connectAiWebSocket() {
            return new Promise((resolve, reject) => {
                const socket = new SockJS('/ws-ai-chat');
                this.aiStompClient = Stomp.over(socket);
                
                this.aiStompClient.connect(
                    {},  // 쿠키 기반 인증 사용
                    () => {
                        this.aiStompClient.subscribe(
                            `/sub/ai-chat/room/${this.aiGameRoomId}`,
                            (message) => {
                                const data = JSON.parse(message.body);
                                if (data.type === 'AI_RESPONSE') {
                                    this.addAiMessage('ai', data.content);
                                    
                                    // AI 메시지에서 [GAME_END] 키워드 감지 (dungeon-game.html 기능)
                                    if (data.content && data.content.includes('[GAME_END]')) {
                                        console.log('🎮 AI 응답에서 [GAME_END] 키워드 감지!');
                                        // [GAME_END] 키워드를 제거한 메시지로 게임 종료 처리
                                        const cleanContent = data.content.replace(/\[GAME_END\]/g, '').trim();
                                        const gameResult = this.analyzeGameResult(cleanContent);
                                        
                                        // 약간의 지연 후 게임 종료 처리 (사용자가 AI 메시지를 읽을 시간 제공)
                                        setTimeout(() => {
                                            this.handleGameEndWithResult(gameResult, cleanContent);
                                        }, 2000);
                                    }
                                } else if (data.type === 'TALK' && data.senderId !== this.currentUser.id) {
                                    this.addAiMessage('other', data.content, data.senderNickname);
                                } else if (data.type === 'GAME_END') {
                                    this.handleGameEnd(data.reason || 'victory');
                                } else if (data.type === 'SYSTEM') {
                                    this.addAiMessage('system', data.content);
                                }
                            }
                        );
                        
                        console.log('AI WebSocket 연결 성공');
                        resolve();
                    },
                    (error) => {
                        console.error('AI WebSocket 연결 실패:', error);
                        reject(error);
                    }
                );
            });
        },
        
        async connectPartyWebSocket(chatRoomId) {
            this.partyRoomId = chatRoomId;
            
            return new Promise((resolve, reject) => {
                const socket = new SockJS('/ws-chat');
                this.partyStompClient = Stomp.over(socket);
                
                this.partyStompClient.connect(
                    {},  // 쿠키 기반 인증 사용
                    () => {
                        this.partyStompClient.subscribe(
                            `/sub/chat/room/${chatRoomId}`,
                            (message) => {
                                const data = JSON.parse(message.body);
                                if (data.type === 'TALK') {
                                    this.addPlayerMessage(
                                        data.senderId === this.currentUser.id ? 'me' : 'other',
                                        data.content,
                                        data.senderNickname
                                    );
                                }
                            }
                        );
                        
                        console.log('파티 채팅 WebSocket 연결 성공');
                        resolve();
                    },
                    (error) => {
                        console.error('파티 채팅 WebSocket 연결 실패:', error);
                        reject(error);
                    }
                );
            });
        },
        
        sendAiMessage() {
            if (!this.aiInput.trim() || !this.aiStompClient) return;
            
            const message = {
                roomId: this.aiGameRoomId,
                senderId: this.currentUser.id,
                senderNickname: this.currentUser.nickname,
                content: this.aiInput,
                type: 'TALK',
                turnNumber: this.currentTurnNumber++
            };
            
            this.aiStompClient.send('/pub/ai-chat/send', {}, JSON.stringify(message));
            this.addAiMessage('user', this.aiInput);
            this.aiInput = '';
        },
        
        requestAiResponse() {
            if (!this.aiStompClient) return;
            
            const message = {
                roomId: this.aiGameRoomId,
                type: 'REQUEST_AI_RESPONSE',
                turnNumber: this.currentTurnNumber
            };
            
            this.aiStompClient.send('/pub/ai-chat/request', {}, JSON.stringify(message));
            this.addAiMessage('system', '🤖 AI 응답을 요청했습니다...');
        },
        
        sendPlayerMessage() {
            if (!this.playerInput.trim() || !this.partyStompClient) return;
            
            const message = {
                roomId: this.partyRoomId,
                senderId: this.currentUser.id,
                content: this.playerInput,
                type: 'TALK'
            };
            
            this.partyStompClient.send('/pub/chat/send', {}, JSON.stringify(message));
            this.playerInput = '';
        },
        
        addAiMessage(sender, content, senderName = null) {
            this.aiMessages.push({
                id: Date.now() + Math.random(),
                sender,
                content,
                senderName,
                time: new Date().toLocaleTimeString()
            });
            
            this.$nextTick(() => {
                const container = document.getElementById('aiMessages');
                if (container) container.scrollTop = container.scrollHeight;
            });
        },
        
        addPlayerMessage(sender, content, senderName = null) {
            this.playerMessages.push({
                id: Date.now() + Math.random(),
                sender,
                content,
                senderName,
                isMe: sender === 'me',
                time: new Date().toLocaleTimeString()
            });
            
            this.$nextTick(() => {
                const container = document.getElementById('playerMessages');
                if (container) container.scrollTop = container.scrollHeight;
            });
        },
        
        formatMessage(content) {
            return content
                .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                .replace(/\*(.*?)\*/g, '<em>$1</em>')
                .replace(/`(.*?)`/g, '<code class="bg-gray-200 dark:bg-gray-700 px-1 rounded">$1</code>')
                .replace(/\n/g, '<br>');
        },
        
        cancelMatching() {
            if (this.matchingStompClient) {
                const message = {
                    type: 'MATCH_CANCEL',
                    userId: this.currentUser.id
                };
                
                this.matchingStompClient.send('/pub/matching/cancel', {}, JSON.stringify(message));
            }
            
            this.isMatching = false;
            this.matchingInfo = '매칭 취소됨';
            Alpine.store('notifications').info('매칭이 취소되었습니다.');
            setTimeout(() => window.location.href = '/game', 2000);
        },
        
        async leaveGame() {
            if (confirm('정말로 게임을 나가시겠습니까?')) {
                await this.handleGameEnd('leave');
            }
        },
        
        async loadCharacterInfo() {
            try {
                const response = await fetch(`/v1/character/${this.currentUser.id}`, {
                    credentials: 'include'  // 쿠키 포함
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.characterData = result.data;
                }
            } catch (error) {
                console.error('캐릭터 정보 로드 오류:', error);
            }
        },
        
        // 압박감 효과 함수들
        triggerPressureEffect(level) {
            const body = document.body;
            
            if (level === 'urgent') {
                // 화면 깜빡임 효과
                body.style.animation = 'pulse-medium 2s ease-in-out 3';
                setTimeout(() => {
                    body.style.animation = '';
                }, 6000);
            } else if (level === 'critical') {
                // 강한 화면 흔들림 효과
                body.style.animation = 'shake 0.5s ease-in-out 5';
                // 붉은 테두리 효과
                body.style.boxShadow = '0 0 50px rgba(239, 68, 68, 0.3)';
                setTimeout(() => {
                    body.style.animation = '';
                    body.style.boxShadow = '';
                }, 2500);
            }
        },
        
        startFinalCountdown() {
            // 마지막 10초 카운트다운
            let countdown = 10;
            const countdownInterval = setInterval(() => {
                if (countdown > 0) {
                    this.addSystemMessage('ai', `⏱️ ${countdown}초!`);
                    // 사운드 효과나 진동 효과 추가 가능
                    if (countdown <= 3) {
                        this.triggerPressureEffect('critical');
                    }
                    countdown--;
                } else {
                    clearInterval(countdownInterval);
                }
            }, 1000);
        },
        
        addSystemMessage(tab, content) {
            const message = {
                id: Date.now(),
                content: content,
                sender: 'SYSTEM',
                timestamp: new Date().toISOString(),
                isSystem: true
            };
            
            if (tab === 'ai') {
                this.aiMessages.push(message);
            } else {
                this.playerMessages.push(message);
            }
            
            // 스크롤 최하단으로
            this.$nextTick(() => {
                const container = tab === 'ai' ? 
                    document.querySelector('#ai-chat-messages') :
                    document.querySelector('#player-chat-messages');
                if (container) {
                    container.scrollTop = container.scrollHeight;
                }
            });
        },
        
        cleanup() {
            this.stopGameTimer();
            
            if (this.matchingStompClient) {
                try { this.matchingStompClient.disconnect(); } catch (e) {}
                this.matchingStompClient = null;
            }
            
            if (this.aiStompClient) {
                try { this.aiStompClient.disconnect(); } catch (e) {}
                this.aiStompClient = null;
            }
            
            if (this.partyStompClient) {
                try { this.partyStompClient.disconnect(); } catch (e) {}
                this.partyStompClient = null;
            }
        }
    }
}