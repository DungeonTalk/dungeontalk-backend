// 모듈화된 던전톡 게임 메인 파일
import { UserAuth } from './modules/auth/userAuth.js';
import { MatchingService } from './modules/matching/matchingService.js';
import { MatchingWebSocket } from './modules/matching/matchingWebSocket.js';
import { GameSession } from './modules/game/gameSession.js';
import { TRPGTimer } from './modules/game/trpgTimer.js';
import { AiChat } from './modules/chat/aiChat.js';
import { PartyChat } from './modules/chat/partyChat.js';
import { MessageHandler } from './modules/chat/messageHandler.js';
import { CharacterService } from './modules/character/characterService.js';
import { CharacterModal } from './modules/character/characterModal.js';
import { ThemeManager } from './modules/ui/themeManager.js';
import { UIHelpers } from './modules/ui/uiHelpers.js';

// 던전톡 게임 애플리케이션 클래스
class DungeonTalkGame {
    constructor() {
        this.initializeModules();
        this.setupEventListeners();
        this.bindGlobalFunctions();
    }

    // 모듈 초기화
    initializeModules() {
        // 기본 서비스
        this.userAuth = new UserAuth();
        this.messageHandler = new MessageHandler();
        this.uiHelpers = new UIHelpers();
        this.themeManager = new ThemeManager();
        
        // 의존성을 가진 서비스
        this.matchingService = new MatchingService(this.userAuth);
        this.matchingWebSocket = new MatchingWebSocket(this.userAuth);
        this.gameSession = new GameSession(this.userAuth);
        this.trpgTimer = new TRPGTimer();
        
        // 채팅 서비스
        this.aiChat = new AiChat(this.userAuth, this.gameSession);
        this.partyChat = new PartyChat(this.userAuth, this.gameSession);
        
        // 캐릭터 서비스
        this.characterService = new CharacterService(this.userAuth);
        this.characterModal = new CharacterModal(this.characterService);
        
        console.log('✅ 모든 모듈이 초기화되었습니다.');
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 초기화 시 로그인 체크
        window.addEventListener('load', () => {
            this.clearAuthDataOnRefresh();
            this.checkExistingLogin();
        });

        // 매칭 WebSocket 메시지 핸들러
        this.setupMatchingHandlers();
        
        // 채팅 메시지 핸들러
        this.setupChatHandlers();
        
        // 타이머 핸들러
        this.setupTimerHandlers();
        
        // UI 이벤트 핸들러
        this.setupUIHandlers();
    }

    // 매칭 핸들러 설정
    setupMatchingHandlers() {
        this.matchingWebSocket.onMessage('QUEUE_STATUS_UPDATE', (message) => {
            this.uiHelpers.updateQueueStatus(message.data);
        });

        this.matchingWebSocket.onMessage('MATCHING_COMPLETE', (message) => {
            this.handleMatchingComplete(message.data);
        });

        this.matchingWebSocket.onMessage('MATCHING_CANCELLED', () => {
            this.handleMatchingCancelled();
        });

        this.matchingWebSocket.onMessage('ERROR', (message) => {
            console.error('매칭 오류:', message.data);
            alert('매칭 오류: ' + message.data);
        });
    }

    // 채팅 핸들러 설정
    setupChatHandlers() {
        // AI 채팅 핸들러
        this.aiChat.onMessage('message', (messageData) => {
            if (messageData.type === 'AI') {
                this.messageHandler.addAiMessage('ai', messageData.content);
                
                // 게임 종료 메시지 체크
                if (messageData.isGameEnd) {
                    setTimeout(() => {
                        this.messageHandler.handleGameEndMessage({
                            content: messageData.content
                        });
                    }, 2000);
                }
            } else if (messageData.type === 'USER') {
                this.messageHandler.addAiMessage('other', `${messageData.senderNickname}: ${messageData.content}`);
            } else if (messageData.type === 'SYSTEM') {
                this.messageHandler.addAiMessage('system', messageData.content);
            }
        });

        this.aiChat.onMessage('myMessage', (messageData) => {
            this.messageHandler.addAiMessage('user', messageData.content);
        });

        // 파티 채팅 핸들러
        this.partyChat.onMessage('userMessage', (messageData) => {
            this.messageHandler.addRightUserMessage(messageData.type, messageData.content);
        });

        this.partyChat.onMessage('systemMessage', (messageData) => {
            this.messageHandler.addRightUserMessage(messageData.type, messageData.content);
        });

        this.partyChat.onMessage('myMessage', (messageData) => {
            this.messageHandler.addRightUserMessage('user', messageData.content);
        });

        this.partyChat.onMessage('participantUpdate', (messageData) => {
            this.messageHandler.addRightUserMessage(messageData.type, messageData.content);
        });
    }

    // 타이머 핸들러 설정
    setupTimerHandlers() {
        // 시간 종료 시 처리
        this.trpgTimer.onTimeEnd(() => {
            console.log('⏰ TRPG 15분 시간 종료!');
            
            // 종료 메시지 추가
            this.messageHandler.addAiMessage('system', '⏰ 15분 게임 시간이 종료되었습니다!');
            this.messageHandler.addAiMessage('system', '🎭 게임 마스터가 마무리 중...');
            
            // 입력 비활성화
            this.uiHelpers.disableChatInputs();
        });

        // 게임 단계 변경 시 처리
        this.trpgTimer.onPhaseChange((newPhase, oldPhase) => {
            const phaseData = this.trpgTimer.getCurrentPhaseInfo();
            console.log(`🎭 게임 단계 변경: ${oldPhase} → ${newPhase} (${phaseData.name})`);
        });

        // 메시지 핸들러에서 게임 종료 시 타이머 정지
        this.messageHandler.onGameEnd(() => {
            if (this.trpgTimer.isRunning()) {
                this.trpgTimer.resetTRPGTimer();
            }
        });
    }

    // UI 이벤트 핸들러 설정
    setupUIHandlers() {
        // 엔터키 핸들러
        this.uiHelpers.setupEnterKeyHandlers();
        this.uiHelpers.setupLoginFormHandlers();
    }

    // 새로고침 시 로컬스토리지 초기화
    clearAuthDataOnRefresh() {
        this.userAuth.clearAuthData();
        console.log('🔄 새로고침 감지 - 로컬스토리지 초기화');
    }

    // 기존 로그인 확인
    checkExistingLogin() {
        const isLoggedIn = this.userAuth.checkExistingLogin();
        
        if (isLoggedIn) {
            this.uiHelpers.showLoggedInState(this.userAuth.getCurrentUser());
            
            // authToken이 설정된 후에 WebSocket 연결
            setTimeout(() => {
                this.connectMatchingWebSocket();
            }, 500);
        }
    }

    // 매칭 WebSocket 연결
    async connectMatchingWebSocket() {
        try {
            await this.matchingWebSocket.connect();
            console.log('✅ 매칭 WebSocket 연결 완료');
        } catch (error) {
            console.error('❌ 매칭 WebSocket 연결 실패:', error);
        }
    }

    // 회원가입
    async register() {
        const userId = document.getElementById('userId')?.value.trim();
        const password = document.getElementById('password')?.value.trim();
        
        if (!userId || !password) {
            alert('ID와 비밀번호를 입력하세요.');
            return;
        }
        
        try {
            const result = await this.userAuth.register(userId, password);
            if (result.success) {
                alert(result.message);
            } else {
                alert('회원가입 실패: ' + result.message);
            }
        } catch (error) {
            alert('회원가입 오류: ' + error.message);
        }
    }

    // 로그인
    async login() {
        const userId = document.getElementById('userId')?.value.trim();
        const password = document.getElementById('password')?.value.trim();
        
        if (!userId || !password) {
            alert('ID와 비밀번호를 입력하세요.');
            return;
        }
        
        try {
            const result = await this.userAuth.login(userId, password);
            if (result.success) {
                this.uiHelpers.showLoggedInState(result.user);
                await this.connectMatchingWebSocket();
            } else {
                alert('로그인 실패: ' + result.message);
            }
        } catch (error) {
            alert('로그인 오류: ' + error.message);
        }
    }

    // 세계관 선택
    selectWorld(worldType) {
        const selectedWorld = this.themeManager.selectWorld(worldType);
        this.matchingService.selectWorld(selectedWorld);
        return selectedWorld;
    }

    // 매칭 시작
    async startMatching() {
        console.log('=== 매칭 시작 메인 함수 디버그 ===');
        const selectedWorld = this.themeManager.getSelectedWorld();
        console.log('themeManager에서 가져온 selectedWorld:', selectedWorld);
        console.log('matchingService.selectedWorld:', this.matchingService.selectedWorld);
        
        if (!selectedWorld) {
            alert('세계관을 선택해주세요.');
            return;
        }

        try {
            const result = await this.matchingService.startMatching();
            if (result.success) {
                this.uiHelpers.updateMatchingUI(true, selectedWorld);
            }
        } catch (error) {
            console.error('매칭 시작 오류:', error);
            alert(error.message);
        }
    }

    // 매칭 취소
    async cancelMatching() {
        try {
            const result = await this.matchingService.cancelMatching();
            if (result.success) {
                this.uiHelpers.updateMatchingUI(false);
            }
        } catch (error) {
            alert(error.message);
        }
    }

    // 매칭 완료 처리
    async handleMatchingComplete(data) {
        console.log('=== 매칭 완료 데이터 디버깅 ===');
        console.log('data:', data);
        console.log('==============================');

        // UI 전환
        this.uiHelpers.showGameScreen(data);

        // 게임 세션 시작
        try {
            await this.startGameSession(data);
        } catch (error) {
            console.error('게임 세션 시작 오류:', error);
            alert('게임 세션 시작 중 오류가 발생했습니다.');
        }
    }

    // 매칭 취소 처리
    handleMatchingCancelled() {
        this.uiHelpers.updateMatchingUI(false);
    }

    // 게임 세션 시작
    async startGameSession(matchData) {
        try {
            const result = await this.gameSession.startGameSession(matchData);
            
            if (result.success) {
                console.log('AI 게임 세션 시작 성공');
                
                // WebSocket 연결들
                await Promise.all([
                    this.aiChat.connect(),
                    this.partyChat.connect()
                ]);
                
                // 입력 활성화
                this.uiHelpers.enableChatInputs();
                
                // 세계관별 환영 메시지
                const selectedWorld = this.themeManager.getSelectedWorld();
                const welcomeMessages = this.themeManager.getWorldSpecificWelcomeMessages(selectedWorld);
                
                welcomeMessages.forEach(msg => {
                    this.messageHandler.addAiMessage(msg.type, msg.content);
                });
                
                this.messageHandler.addRightUserMessage('system', '💬 플레이어들과의 채팅이 시작되었습니다!');
                
                // 참여자 수 표시
                const participantCount = matchData.participants ? matchData.participants.length : 1;
                this.messageHandler.addRightUserMessage('system', `👥 총 ${participantCount}명의 모험가가 함께합니다!`);
                
                // 캐릭터 스탯 로딩 결과 메시지
                const statsResult = await this.gameSession.loadCharacterStats();
                if (statsResult.success) {
                    this.messageHandler.addAiMessage('system', '⚔️ 캐릭터 정보가 AI에게 전달되었습니다. 스탯 기반 게임이 시작됩니다!');
                    
                    const characterStats = this.gameSession.getCharacterStats();
                    if (characterStats && characterStats.raceName) {
                        this.messageHandler.addAiMessage('system', `🎮 캐릭터: ${characterStats.raceName} ${characterStats.level || 1}레벨`);
                    }
                } else {
                    this.messageHandler.addAiMessage('system', statsResult.message);
                }
                
                // 타이머 시작 (2초 후)
                setTimeout(() => {
                    if (this.gameSession.isGameSessionStarted() && !this.trpgTimer.getGameStartTime()) {
                        this.trpgTimer.startTRPGTimer();
                    }
                }, 2000);
                
            } else {
                throw new Error(result.message || '게임 세션 시작 실패');
            }
        } catch (error) {
            console.error('❌ 게임 세션 시작 오류:', error);
            
            if (error.message.includes('인증')) {
                this.userAuth.clearAuthData();
                this.showLoginModal();
            } else {
                alert('게임 세션 시작 오류: ' + error.message);
            }
        }
    }

    // AI 메시지 전송
    sendAiMessage() {
        const input = document.getElementById('aiMessageInput');
        const message = input?.value.trim();
        
        if (!message) return;
        
        this.aiChat.sendMessage(message);
        input.value = '';
    }

    // AI 응답 요청
    async requestAiResponse() {
        try {
            await this.gameSession.requestAiResponse();
        } catch (error) {
            console.error('AI 응답 요청 오류:', error);
            alert('AI 응답 요청 중 오류가 발생했습니다.');
        }
    }

    // 파티 메시지 전송
    sendRightUserMessage() {
        const input = document.getElementById('rightUserMessageInput');
        const message = input?.value.trim();
        
        if (!message) return;
        
        this.partyChat.sendMessage(message);
        input.value = '';
    }

    // 게임 나가기
    async leaveGame() {
        if (!confirm('정말로 게임을 나가시겠습니까? 진행 중인 게임이 종료됩니다.')) {
            return;
        }
        
        try {
            // WebSocket 퇴장 메시지 전송
            this.aiChat.sendLeaveMessage();
            this.partyChat.sendLeaveMessage();
            
            // 게임 세션 종료
            await this.gameSession.leaveGame();
            
            // WebSocket 연결 해제
            this.aiChat.disconnect();
            this.partyChat.disconnect();
            
            // 타이머 정지
            this.trpgTimer.resetTRPGTimer();
            
            // 채팅 비활성화
            this.uiHelpers.disableChatInputs();
            
            // 채팅 메시지 초기화
            this.messageHandler.clearMessages();
            
            // UI를 매칭 화면으로 복귀
            this.uiHelpers.showMatchingScreen(this.themeManager.getSelectedWorld());
            
            // 매칭 WebSocket 재연결
            setTimeout(() => {
                this.connectMatchingWebSocket();
            }, 1000);
            
        } catch (error) {
            console.error('❌ 게임 나가기 오류:', error);
            alert('게임 나가기 중 오류가 발생했습니다: ' + error.message);
        }
    }

    // 로그아웃
    logout() {
        const confirmed = confirm('정말로 로그아웃하시겠습니까?');
        if (!confirmed) return;
        
        // WebSocket 연결 해제
        this.matchingWebSocket.disconnect();
        this.aiChat.disconnect();
        this.partyChat.disconnect();
        
        // 타이머 리셋
        this.trpgTimer.resetTRPGTimer();
        
        // 인증 데이터 클리어
        this.userAuth.logout();
        
        // 테마 리셋
        this.themeManager.resetTheme();
        
        // UI 초기화
        document.getElementById('headerActions').style.display = 'none';
        document.getElementById('loginSection').style.display = 'block';
        document.getElementById('matchingSection').classList.add('hidden');
        document.getElementById('gameSection').classList.add('hidden');
        
        // 모달들 닫기
        this.characterModal.hideAllModals();
    }

    // 로그인 모달 표시 (에러 시)
    showLoginModal() {
        // 간단한 구현, 실제로는 별도 모달 구현 필요
        alert('로그인이 필요합니다. 페이지를 새로고침하여 다시 로그인해주세요.');
        window.location.reload();
    }

    // 전역 함수 바인딩
    bindGlobalFunctions() {
        // 인증 관련
        window.register = () => this.register();
        window.login = () => this.login();
        window.logout = () => this.logout();
        
        // 매칭 관련
        window.selectWorld = (worldType) => this.selectWorld(worldType);
        window.startMatching = () => this.startMatching();
        window.cancelMatching = () => this.cancelMatching();
        
        // 채팅 관련
        window.sendAiMessage = () => this.sendAiMessage();
        window.requestAiResponse = () => this.requestAiResponse();
        window.sendRightUserMessage = () => this.sendRightUserMessage();
        
        // 게임 관련
        window.leaveGame = () => this.leaveGame();
        
        // 캐릭터 모달 관련 (characterModal에서 바인딩)
        this.characterModal.bindGlobalFunctions();
        
        // 테마 관련 (themeManager에서 바인딩)  
        this.themeManager.bindGlobalFunctions();
        
        console.log('✅ 전역 함수 바인딩 완료');
    }
}

// 애플리케이션 시작
document.addEventListener('DOMContentLoaded', () => {
    console.log('🎮 던전톡 게임 시작');
    window.dungeonTalkGame = new DungeonTalkGame();
});