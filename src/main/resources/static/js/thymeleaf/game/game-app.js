// Game Application Module
function gameApp() {
    const app = {
        // Auth & User
        authToken: null,
        currentUser: null,
        
        // Game Status
        gameStatus: '대기 중',
        matchingInfo: '대기 중',
        roomInfo: '미연결',
        gameStarted: false,
        
        // Matching
        selectedWorld: null,
        isMatching: false,
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
        gameTime: 900, // 15분 = 900초
        gameInterval: null,
        gamePhase: 'relaxed', // relaxed, normal, urgent, critical
        gameEndReason: null,
        
        // Character System
        showCharacterModal: false,
        showCharacterInfo: false,
        selectedRace: '',
        raceDescription: '',
        characterData: null,
        
        init() {
            // notifications store가 없으면 생성 (폴백)
            if (typeof Alpine !== 'undefined' && !Alpine.store('notifications')) {
                Alpine.store('notifications', {
                    success(msg) { console.log('Success:', msg); },
                    error(msg) { console.error('Error:', msg); },
                    warning(msg) { console.warn('Warning:', msg); },
                    info(msg) { console.info('Info:', msg); }
                });
            }
            
            // Spring Security로 이미 인증됨 - 토큰만 가져오기
            this.initializeUser();
            this.loadCharacterData();
            
            // Handle page unload
            window.addEventListener('beforeunload', () => this.cleanup());
        },
        
        async loadCharacterData() {
            try {
                const response = await fetch('/v2/characters/my', {
                    credentials: 'include'  // 쿠키 포함
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.characterData = result.data;
                    console.log('캐릭터 데이터 로드:', this.characterData);
                }
            } catch (error) {
                console.error('캐릭터 데이터 로드 실패:', error);
            }
        },
        
        // Timer Functions
        formatTime(seconds) {
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        },
        
        updateGamePhase() {
            if (this.gameTime >= 600) {
                this.gamePhase = 'relaxed'; // 10분 이상
            } else if (this.gameTime >= 300) {
                this.gamePhase = 'normal'; // 5-10분
            } else if (this.gameTime >= 60) {
                this.gamePhase = 'urgent'; // 1-5분
            } else {
                this.gamePhase = 'critical'; // 1분 미만
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
            return messages[phase] || '대기';
        },
        
        startGameTimer() {
            this.gameTime = 900; // 15분 리셋
            this.updateGamePhase();
            
            if (this.gameInterval) {
                clearInterval(this.gameInterval);
            }
            
            this.gameInterval = setInterval(() => {
                this.gameTime--;
                this.updateGamePhase();
                
                // 타이머 종료
                if (this.gameTime <= 0) {
                    clearInterval(this.gameInterval);
                    this.gameInterval = null;
                    this.handleGameEnd('timeout');
                }
                
                // 경고 메시지
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
        
        initializeUser() {
            // Spring Security로 이미 인증되었으므로, 토큰과 사용자 정보만 가져오기
            // 쿠키에서 토큰 확인
            const cookies = document.cookie.split(';');
            for (let cookie of cookies) {
                const [name, value] = cookie.trim().split('=');
                if (name === 'accessToken') {
                    this.authToken = value;
                    break;
                }
            }
            
            // 토큰이 없으면 localStorage 확인 (클라이언트 로그인)
            if (!this.authToken) {
                this.authToken = localStorage.getItem('authToken');
            }
            
            // 사용자 정보 가져오기
            let userInfo = localStorage.getItem('currentUser');
            
            // 사용자 정보가 없고 토큰이 있으면 토큰에서 추출
            if (!userInfo && this.authToken) {
                try {
                    const tokenPayload = JSON.parse(atob(this.authToken.split('.')[1]));
                    console.log('JWT 토큰 페이로드:', tokenPayload);
                    
                    userInfo = JSON.stringify({
                        id: tokenPayload.memberId || tokenPayload.sub || tokenPayload.id,
                        memberId: tokenPayload.memberId || tokenPayload.sub || tokenPayload.id,
                        nickname: tokenPayload.nickname || tokenPayload.name || '플레이어'
                    });
                    localStorage.setItem('currentUser', userInfo);
                } catch (e) {
                    console.error('JWT 토큰 파싱 오류:', e);
                }
            }
            
            // 사용자 정보 설정
            if (userInfo) {
                try {
                    this.currentUser = JSON.parse(userInfo);
                    if (!this.currentUser.memberId && this.currentUser.id) {
                        this.currentUser.memberId = this.currentUser.id;
                    }
                    console.log('현재 사용자:', this.currentUser);
                } catch (e) {
                    console.error('사용자 정보 파싱 오류:', e);
                }
            }
            
            // 기본값 설정 (토큰/사용자 정보가 없는 경우)
            if (!this.currentUser) {
                this.currentUser = {
                    id: 'guest_' + Date.now(),
                    memberId: 'guest_' + Date.now(),
                    nickname: '게스트'
                };
            }
            
            this.gameStatus = '준비됨';
            Alpine.store('notifications').success('게임 준비 완료!');
        },
        
        selectWorld(worldId, worldName) {
            this.selectedWorld = worldId;
            Alpine.store('notifications').success(`${worldName} 세계관이 선택되었습니다.`);
            
            // 세계관 선택 후 게임 페이지로 이동 준비
            sessionStorage.setItem('selectedWorld', JSON.stringify({
                id: worldId,
                name: worldName
            }));
            
            // 버튼 상태 강제 업데이트
            const button = document.querySelector('button[\\@click="startMatching()"]');
            if (button && this.selectedWorld) {
                button.removeAttribute('disabled');
            }
        },
        
        async startMatching() {
            if (!this.selectedWorld) {
                Alpine.store('notifications').warning('세계관을 먼저 선택해주세요.');
                return;
            }
            
            // 캐릭터 존재 여부 체크
            try {
                const hasCharacter = await this.checkCharacterExists();
                if (!hasCharacter) {
                    // 캐릭터가 없으면 경고 메시지 표시
                    Alpine.store('notifications').error('캐릭터가 없습니다. 먼저 "⚔️ 내 캐릭터" 버튼을 클릭하여 캐릭터를 생성해주세요.');
                    
                    // 캐릭터 모달 자동 표시 (선택사항)
                    if (this.showCharacterModal !== undefined) {
                        this.showCharacterModal = true;
                    }
                    return;
                }
            } catch (error) {
                console.error('캐릭터 체크 실패:', error);
                Alpine.store('notifications').error('캐릭터 정보 확인 중 오류가 발생했습니다.');
                return;
            }
            
            // 게임 플레이 페이지로 이동
            window.location.href = `/game/play?world=${this.selectedWorld}`;
        },
        
        async checkCharacter() {
            try {
                const response = await fetch('/v2/characters/my', {
                    credentials: 'include'  // 쿠키 포함
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.data) {
                        this.characterData = result.data;
                        return true;
                    }
                }
            } catch (error) {
                console.error('캐릭터 확인 오류:', error);
            }
            return false;
        },
        
        // 캐릭터 존재 여부만 체크하는 메서드 (dungeon-game.html 참고)
        async checkCharacterExists() {
            try {
                const response = await fetch('/v2/characters/my', {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.ok) {
                    const result = await response.json();
                    console.log('캐릭터 존재 여부 체크 결과:', result);
                    
                    // 캐릭터 데이터가 존재하는지 확인
                    if (result.data && result.data.id) {
                        this.characterData = result.data;
                        return true;
                    }
                } else if (response.status === 404) {
                    console.log('캐릭터가 존재하지 않습니다.');
                    return false;
                } else {
                    console.error('캐릭터 체크 API 오류:', response.status);
                }
            } catch (error) {
                console.error('캐릭터 존재 여부 체크 오류:', error);
                throw error;
            }
            return false;
        },
        
        async createCharacter() {
            console.log('createCharacter 함수 호출됨');
            console.log('선택된 종족:', this.selectedRace);
            
            if (!this.selectedRace) {
                Alpine.store('notifications').warning('종족을 선택해주세요.');
                return;
            }
            
            // V2 API는 memberId가 필요없음 - 인증 정보에서 자동 추출
            const requestBody = {
                race: this.selectedRace
            };
            
            console.log('요청 데이터:', requestBody);
            console.log('인증 토큰:', this.authToken);
            
            try {
                const response = await fetch('/v2/characters', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include',  // 쿠키 포함
                    body: JSON.stringify(requestBody)
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.characterData = result.data || result;
                    this.showCharacterModal = false;
                    Alpine.store('notifications').success('캐릭터가 생성되었습니다!');
                    
                    // 캐릭터 데이터 새로 로드
                    await this.loadCharacterData();
                    
                    // 매칭 시작
                    if (this.selectedWorld) {
                        await this.startMatching();
                    }
                } else {
                    const errorData = await response.json();
                    console.error('캐릭터 생성 실패:', errorData);
                    Alpine.store('notifications').error(errorData.message || '캐릭터 생성에 실패했습니다.');
                }
            } catch (error) {
                console.error('캐릭터 생성 오류:', error);
                // notifications store가 있는지 확인
                if (Alpine.store('notifications') && Alpine.store('notifications').error) {
                    Alpine.store('notifications').error('캐릭터 생성 중 오류가 발생했습니다.');
                } else {
                    console.error('캐릭터 생성 중 오류가 발생했습니다.');
                }
            }
        },
        
        updateRaceDescription() {
            const descriptions = {
                'HUMAN': '인간은 모든 능력치가 균형있게 분포되어 있어 어떤 상황에도 잘 적응합니다. 다양한 플레이 스타일이 가능합니다.',
                'ELF': '엘프는 뛰어난 민첩성과 지혜를 가지고 있습니다. 빠른 반응과 마법적 감각이 필요한 상황에서 강점을 보입니다.',
                'DWARF': '드워프는 강인한 체력과 의지력을 자랑합니다. 긴 전투와 험난한 환경에서도 버틸 수 있습니다.',
                'ORC': '오크는 압도적인 힘을 가지고 있습니다. 직접적인 전투에서 강력한 위력을 발휘합니다.'
            };
            this.raceDescription = descriptions[this.selectedRace] || '';
        },
        
        cleanup() {
            // WebSocket 연결 정리
            if (this.matchingStompClient) {
                try { this.matchingStompClient.disconnect(); } catch (e) {}
                this.matchingStompClient = null;
            }
        }
    };
    
    return app;
}