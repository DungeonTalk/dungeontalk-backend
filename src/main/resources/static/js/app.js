import { LoginComponent } from './components/LoginComponent.js';
import { CharacterModal } from './components/CharacterModal.js';
import { WorldSelector } from './components/WorldSelector.js';
import { GameStatus } from './components/GameStatus.js';
import { ChatWindow } from './components/ChatWindow.js';

const { createApp } = Vue;

// 상수 정의
const CONSTANTS = {
    GAME_DURATION: 900, // 15분 = 900초
    TIMER_INTERVAL: 1000, // 1초
    WEBSOCKET_ENDPOINT: '/ws-chat',
    RECONNECT_DELAY: 3000, // 3초
    MAX_RECONNECT_ATTEMPTS: 5
};

// 메인 애플리케이션
const app = createApp({
    components: {
        'login-component': LoginComponent,
        'character-modal': CharacterModal,
        'world-selector': WorldSelector,
        'game-status': GameStatus,
        'chat-window': ChatWindow
    },
    
    data() {
        return {
            // 사용자 정보
            isLoggedIn: false,
            currentUser: null,
            accessToken: null,
            memberId: null,
            
            // 캐릭터 정보
            character: null,
            showCharacterModal: false,
            
            // 월드 정보
            currentWorld: null,
            inGame: false,
            
            // 게임 상태
            gameTimer: null,
            timerInterval: null,
            currentHp: 100,
            maxHp: 100,
            currentMp: 50,
            maxMp: 50,
            
            // 채팅
            roomId: null,
            roomType: 'CHAT',
            
            // WebSocket
            stompClient: null,
            wsConnected: false,
            reconnectAttempts: 0,
            isReconnecting: false,
            
            // UI 상태
            loading: false,
            error: null,
            showWorldSelector: false
        };
    },
    
    computed: {
        username() {
            return this.currentUser?.username || 'Guest';
        }
    },
    
    mounted() {
        this.initializeApp();
        this.setupEventListeners();
        this.initializeAnimations();
    },
    
    beforeUnmount() {
        this.cleanup();
    },
    
    methods: {
        // 앱 초기화
        async initializeApp() {
            // 저장된 토큰 확인
            const savedToken = localStorage.getItem('accessToken');
            if (savedToken) {
                this.accessToken = savedToken;
                await this.validateToken();
            }
            
            // 배경 애니메이션 시작
            this.startBackgroundAnimation();
        },
        
        // 토큰 검증
        async validateToken() {
            try {
                const response = await fetch('/v1/member/status/me', {
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
                
                if (response.ok) {
                    const data = await response.json();
                    this.isLoggedIn = true;
                    this.currentUser = data.data;
                    this.memberId = data.data.memberId;
                    
                    if (data.data.hasCharacter) {
                        await this.loadCharacter();
                    }
                    
                    this.connectWebSocket();
                } else {
                    this.logout();
                }
            } catch (error) {
                console.error('Token validation error:', error);
                this.logout();
            }
        },
        
        // 로그인 성공 처리
        handleLoginSuccess(loginData) {
            this.isLoggedIn = true;
            this.accessToken = loginData.token;
            
            // JWT에서 추출한 사용자 정보 사용
            if (loginData.userInfo) {
                this.currentUser = {
                    username: loginData.username,
                    id: loginData.userInfo.id,
                    name: loginData.userInfo.name
                };
                this.memberId = loginData.userInfo.id;
            } else {
                this.currentUser = { username: loginData.username };
            }
            
            // 회원 정보 로드
            this.loadMemberInfo();
            
            // WebSocket 연결
            this.connectWebSocket();
        },
        
        // 회원 정보 로드
        async loadMemberInfo() {
            try {
                const response = await fetch('/v1/member/status/me', {
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
                
                if (response.ok) {
                    const data = await response.json();
                    this.currentUser = data.data;
                    this.memberId = data.data.memberId;
                    
                    if (data.data.hasCharacter) {
                        await this.loadCharacter();
                    } else {
                        this.showCharacterModal = true;
                    }
                }
            } catch (error) {
                console.error('Member info load error:', error);
            }
        },
        
        // 캐릭터 로드
        async loadCharacter() {
            try {
                // V2 API 사용 - 인증된 사용자의 캐릭터 정보 직접 조회
                const response = await fetch('/v2/characters/my', {
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
                
                if (response.ok) {
                    const data = await response.json();
                    this.character = data.data;
                    
                    // HP/MP 설정
                    if (data.data?.calculatedStats) {
                        this.maxHp = data.data.calculatedStats.maxHp;
                        this.currentHp = this.maxHp;
                        this.maxMp = data.data.calculatedStats.maxMp;
                        this.currentMp = this.maxMp;
                    }
                } else if (response.status === 404) {
                    // 캐릭터가 없는 경우
                    this.character = null;
                    this.showCharacterModal = true;
                }
            } catch (error) {
                console.error('Character load error:', error);
            }
        },
        
        // WebSocket 연결
        connectWebSocket() {
            if (!this.accessToken) return;
            
            const socket = new SockJS(CONSTANTS.WEBSOCKET_ENDPOINT);
            this.stompClient = Stomp.over(socket);
            
            // 디버그 모드 비활성화
            this.stompClient.debug = null;
            
            this.stompClient.connect(
                { 'Authorization': `Bearer ${this.accessToken}` },
                () => {
                    console.log('WebSocket connected');
                    this.wsConnected = true;
                    this.reconnectAttempts = 0;
                },
                (error) => {
                    console.error('WebSocket error:', error);
                    this.wsConnected = false;
                    this.handleReconnect();
                }
            );
        },
        
        // 재연결 처리
        handleReconnect() {
            // 이미 재연결 중이거나 최대 시도 횟수 초과 시 중단
            if (this.isReconnecting || this.reconnectAttempts >= CONSTANTS.MAX_RECONNECT_ATTEMPTS) {
                if (this.reconnectAttempts >= CONSTANTS.MAX_RECONNECT_ATTEMPTS) {
                    this.error = '서버와의 연결이 끊어졌습니다. 페이지를 새로고침해주세요.';
                }
                return;
            }
            
            this.isReconnecting = true;
            this.reconnectAttempts++;
            
            setTimeout(() => {
                try {
                    this.connectWebSocket();
                    // 연결 성공 시 재연결 상태 초기화
                    this.reconnectAttempts = 0;
                } catch (error) {
                    console.error('WebSocket 재연결 오류:', error);
                    // 재귀 호출 대신 다음 재연결은 connectWebSocket의 error 핸들러에서 처리
                } finally {
                    this.isReconnecting = false;
                }
            }, CONSTANTS.RECONNECT_DELAY);
        },
        
        // 월드 입장
        handleWorldJoined(data) {
            this.currentWorld = data.world;
            this.roomId = data.matchingData?.roomId;
            this.roomType = data.world.worldType === 'AI' ? 'AI' : 'CHAT';
            this.inGame = true;
            this.showWorldSelector = false;
            
            // 게임 타이머 시작
            this.startGameTimer();
        },
        
        // 게임 타이머 시작
        startGameTimer() {
            this.gameTimer = CONSTANTS.GAME_DURATION;
            
            this.timerInterval = setInterval(() => {
                if (this.gameTimer > 0) {
                    this.gameTimer--;
                } else {
                    this.endGame();
                }
            }, CONSTANTS.TIMER_INTERVAL);
        },
        
        // 게임 종료
        endGame() {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
            this.inGame = false;
            
            // 게임 종료 메시지 전송
            if (this.stompClient && this.roomId) {
                const destination = this.roomType === 'AI' 
                    ? '/pub/aichat/game/end'
                    : '/pub/room/game/end';
                
                this.stompClient.send(destination, {}, JSON.stringify({
                    roomId: this.roomId,
                    characterId: this.character?.id
                }));
            }
            
            this.roomId = null;
            this.currentWorld = null;
        },
        
        // 로그아웃
        async logout() {
            // 서버에 로그아웃 요청
            try {
                await fetch('/v1/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
            } catch (error) {
                console.error('Logout error:', error);
            }
            
            // 클라이언트 상태 초기화
            this.cleanup();
            
            // 로컬 스토리지 정리
            localStorage.removeItem('accessToken');
            
            // 상태 초기화
            this.isLoggedIn = false;
            this.currentUser = null;
            this.accessToken = null;
            this.memberId = null;
            this.character = null;
            this.currentWorld = null;
            this.inGame = false;
        },
        
        // 정리 작업
        cleanup() {
            // 타이머 정리
            if (this.timerInterval) {
                clearInterval(this.timerInterval);
                this.timerInterval = null;
            }
            
            // WebSocket 연결 해제
            if (this.stompClient) {
                this.stompClient.disconnect();
                this.stompClient = null;
            }
        },
        
        // 이벤트 리스너 설정
        setupEventListeners() {
            // 페이지 이탈 시 정리
            window.addEventListener('beforeunload', () => {
                this.cleanup();
            });
            
            // 키보드 단축키
            document.addEventListener('keydown', (e) => {
                if (!this.isLoggedIn) return;
                
                // C: 캐릭터 창
                if (e.key === 'c' || e.key === 'C') {
                    this.showCharacterModal = !this.showCharacterModal;
                }
                
                // W: 월드 선택
                if (e.key === 'w' || e.key === 'W') {
                    if (!this.inGame) {
                        this.showWorldSelector = !this.showWorldSelector;
                    }
                }
                
                // ESC: 창 닫기
                if (e.key === 'Escape') {
                    this.showCharacterModal = false;
                    this.showWorldSelector = false;
                }
            });
        },
        
        // 애니메이션 초기화
        initializeAnimations() {
            // GSAP 애니메이션 설정
            if (typeof gsap !== 'undefined') {
                // 파티클 애니메이션
                gsap.to('.particle', {
                    y: -100,
                    opacity: 0,
                    duration: 3,
                    stagger: 0.2,
                    repeat: -1,
                    ease: 'power2.out'
                });
                
                // 배경 그라디언트 애니메이션
                gsap.to('.animated-bg', {
                    backgroundPosition: '100% 100%',
                    duration: 20,
                    repeat: -1,
                    yoyo: true,
                    ease: 'none'
                });
            }
        },
        
        // 배경 애니메이션 시작
        startBackgroundAnimation() {
            // 파티클 생성
            const particleContainer = document.querySelector('.particle-container');
            if (particleContainer) {
                for (let i = 0; i < 30; i++) {
                    const particle = document.createElement('div');
                    particle.className = 'particle';
                    particle.style.left = Math.random() * 100 + '%';
                    particle.style.animationDelay = Math.random() * 5 + 's';
                    particleContainer.appendChild(particle);
                }
            }
        }
    }
});

// 앱 마운트
app.mount('#app');