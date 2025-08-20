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
                'relaxed': '시작 단계',
                'normal': '진행 단계',
                'urgent': '클라이맥스',
                'critical': '최종 단계'
            };
            return texts[phase] || '준비 중';
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
                
                if (this.gameTime === 300) {
                    Alpine.store('notifications').warning('⏰ 5분 남았습니다! 서둘러 임무를 완수하세요!');
                } else if (this.gameTime === 60) {
                    Alpine.store('notifications').error('⚠️ 1분 남았습니다! 마지막 기회입니다!');
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
                case 'MATCH_WAITING':
                    this.matchingStatusText = '매칭을 찾는 중...';
                    this.queueInfo = `대기 중인 플레이어: ${message.queueSize || 0}명`;
                    break;
                    
                case 'MATCH_FOUND':
                    this.matchingStatusText = '매칭 찾기 완료! 게임을 준비 중...';
                    Alpine.store('notifications').success('매칭이 완료되었습니다!');
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
                    
                case 'MATCH_FAILED':
                    this.isMatching = false;
                    this.matchingInfo = '매칭 실패';
                    Alpine.store('notifications').error(message.reason || '매칭에 실패했습니다.');
                    setTimeout(() => window.location.href = '/game', 3000);
                    break;
            }
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
                    endMessage = '🎉 축하합니다! 임무를 성공적으로 완수했습니다!';
                    isSuccess = true;
                    break;
                case 'defeat':
                    endMessage = '💀 안타깝습니다. 임무에 실패했습니다.';
                    break;
                case 'leave':
                    endMessage = '👋 게임을 나갔습니다.';
                    break;
                default:
                    endMessage = '게임이 종료되었습니다.';
            }
            
            this.addAiMessage('system', endMessage);
            this.addPlayerMessage('system', endMessage);
            
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
            
            this.gameStatus = '게임 종료';
            Alpine.store('notifications').info(endMessage);
            
            setTimeout(() => {
                window.location.href = '/game';
            }, 3000);
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
        
        cleanup() {
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