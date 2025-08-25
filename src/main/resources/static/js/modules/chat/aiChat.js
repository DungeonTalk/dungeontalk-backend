// AI 채팅 관리
export class AiChat {
    constructor(auth, gameSession) {
        this.auth = auth;
        this.gameSession = gameSession;
        this.stompClient = null;
        this.messageHandlers = new Map();
    }

    // AI WebSocket 연결
    connect() {
        const aiGameRoomId = this.gameSession.getAiGameRoomId();
        const authToken = this.auth.getAuthToken();
        
        console.log('=== AI WebSocket 연결 시도 ===');
        console.log('authToken:', authToken ? 'exists' : 'null');
        console.log('aiGameRoomId:', aiGameRoomId);
        
        // 기존 AI WebSocket 연결 해제
        if (this.stompClient && this.stompClient.connected) {
            console.log('🔄 기존 AI WebSocket 연결 해제 중...');
            try {
                this.stompClient.disconnect();
                console.log('✅ 기존 AI WebSocket 연결 해제 완료');
            } catch (error) {
                console.error('❌ 기존 AI WebSocket 해제 오류:', error);
            }
        }
        
        if (!authToken) {
            console.error('AI WebSocket 연결 실패: 인증 토큰이 없습니다.');
            return Promise.reject(new Error('인증 토큰이 없습니다.'));
        }
        
        if (!aiGameRoomId) {
            console.error('AI WebSocket 연결 실패: aiGameRoomId가 없습니다.');
            return Promise.reject(new Error('AI 게임룸 ID가 없습니다.'));
        }
        
        return new Promise((resolve, reject) => {
            try {
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=' + encodeURIComponent(aiGameRoomId));
                this.stompClient = Stomp.over(socket);
                
                this.stompClient.connect({}, (frame) => {
                    console.log('🤖 AI 채팅 WebSocket 연결 성공');
                    
                    // AI 채팅방 구독 (단일 구독)
                    const subscription = this.stompClient.subscribe(`/sub/aichat/room/${aiGameRoomId}`, (message) => {
                        console.log('=== AI WebSocket 메시지 수신 ===');
                        console.log('Raw message:', message);
                        console.log('Message body:', message.body);
                        const aiMessage = JSON.parse(message.body);
                        console.log('Parsed aiMessage:', aiMessage);
                        this.handleAiMessage(aiMessage);
                        console.log('=========================================');
                    });
                    
                    console.log('✅ AI 채팅방 구독 완료:', `/sub/aichat/room/${aiGameRoomId}`);
                    resolve();
                    
                }, (error) => {
                    console.error('❌ AI 채팅 WebSocket 연결 실패:', error);
                    reject(error);
                });
            } catch (error) {
                console.error('❌ AI WebSocket 오류:', error);
                reject(error);
            }
        });
    }

    // AI 메시지 처리
    handleAiMessage(message) {
        const currentUser = this.auth.getCurrentUser();
        
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
        
        // 메시지 타입별 처리
        const messageData = {
            type: message.messageType,
            content: message.content,
            senderNickname: message.senderNickname,
            senderId: message.senderId
        };

        // AI 메시지에서 [GAME_END] 키워드 감지
        if (message.messageType === 'AI' && message.content && message.content.includes('[GAME_END]')) {
            console.log('🎮 AI 응답에서 [GAME_END] 키워드 감지!');
            messageData.isGameEnd = true;
            messageData.content = message.content.replace(/\[GAME_END\]/g, '').trim();
        }

        this.notifyHandlers('message', messageData);
    }

    // 메시지 전송
    sendMessage(message) {
        const aiGameRoomId = this.gameSession.getAiGameRoomId();
        const currentUser = this.auth.getCurrentUser();
        const characterStats = this.gameSession.getCharacterStats();
        const currentTurnNumber = this.gameSession.getCurrentTurnNumber();
        
        if (!message.trim()) return;
        
        // WebSocket으로 전송
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send('/pub/room/ai/send', {}, JSON.stringify({
                roomId: aiGameRoomId,
                roomType: 'AI_GAME',
                senderId: currentUser.id,
                messageType: 'USER',
                content: message,
                aiGameRoomId: aiGameRoomId,
                gameActionType: 'CHAT',
                turnNumber: currentTurnNumber,
                characterStats: characterStats
            }));
            
            // 내 메시지 표시를 위해 핸들러에 알림
            this.notifyHandlers('myMessage', { content: message });
        } else {
            console.warn('AI WebSocket이 연결되지 않음');
        }
    }

    // 게임 종료 메시지 처리
    handleGameEndMessage(message) {
        console.log('🎮 게임 종료 메시지 수신:', message);
        
        // 게임 결과 분석
        const gameResult = this.analyzeGameResult(message.content);
        
        // 핸들러에 게임 종료 알림
        this.notifyHandlers('gameEnd', {
            content: message.content,
            result: gameResult
        });
    }

    // 게임 결과 분석
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
    }

    // 퇴장 메시지 전송
    sendLeaveMessage() {
        const aiGameRoomId = this.gameSession.getAiGameRoomId();
        const currentUser = this.auth.getCurrentUser();
        
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.send('/pub/room/ai/send', {}, JSON.stringify({
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
    }

    // 메시지 핸들러 등록
    onMessage(type, handler) {
        if (!this.messageHandlers.has(type)) {
            this.messageHandlers.set(type, []);
        }
        this.messageHandlers.get(type).push(handler);
    }

    // 메시지 핸들러 제거
    offMessage(type, handler) {
        if (this.messageHandlers.has(type)) {
            const handlers = this.messageHandlers.get(type);
            const index = handlers.indexOf(handler);
            if (index > -1) {
                handlers.splice(index, 1);
            }
        }
    }

    // 핸들러 알림
    notifyHandlers(type, data) {
        const handlers = this.messageHandlers.get(type) || [];
        handlers.forEach(handler => {
            try {
                handler(data);
            } catch (error) {
                console.error('AI 채팅 핸들러 오류:', error);
            }
        });
    }

    // 연결 해제
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.disconnect();
                console.log('✅ AI WebSocket 연결 해제');
            } catch (error) {
                console.error('❌ AI WebSocket 해제 오류:', error);
            }
        }
        this.stompClient = null;
        this.messageHandlers.clear();
    }

    // 연결 상태 확인
    isConnected() {
        return this.stompClient && this.stompClient.connected;
    }
}