// 매칭 WebSocket 연결 관리
export class MatchingWebSocket {
    constructor(auth) {
        this.auth = auth;
        this.stompClient = null;
        this.messageHandlers = new Map();
    }

    // 매칭 WebSocket 연결
    connect() {
        if (this.stompClient && this.stompClient.connected) {
            return Promise.resolve();
        }
        
        const authToken = this.auth.getAuthToken();
        if (!authToken) {
            return Promise.reject(new Error('인증 토큰이 없습니다'));
        }

        return new Promise((resolve, reject) => {
            try {
                console.log('매칭 WebSocket 연결 시도 중...', '/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=matching');
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=matching');
                this.stompClient = Stomp.over(socket);
                
                this.stompClient.connect({}, (frame) => {
                    console.log('✅ 매칭 WebSocket 연결 성공:', frame);
                    
                    // 개인 매칭 알림 구독
                    const currentUser = this.auth.getCurrentUser();
                    const subscriptionPath = `/sub/matching/user/${currentUser.id}`;
                    console.log('매칭 알림 구독:', subscriptionPath);
                    
                    this.stompClient.subscribe(subscriptionPath, (message) => {
                        console.log('매칭 메시지 수신:', message.body);
                        this.handleMessage(JSON.parse(message.body));
                    });
                    
                    resolve();
                }, (error) => {
                    console.error('❌ 매칭 WebSocket 연결 실패:', error);
                    reject(error);
                });
            } catch (error) {
                console.error('매칭 WebSocket 오류:', error);
                reject(error);
            }
        });
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

    // 메시지 처리
    handleMessage(message) {
        const handlers = this.messageHandlers.get(message.type) || [];
        handlers.forEach(handler => {
            try {
                handler(message);
            } catch (error) {
                console.error('매칭 메시지 핸들러 오류:', error);
            }
        });
    }

    // 연결 해제
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.disconnect();
                console.log('✅ 매칭 WebSocket 연결 해제');
            } catch (error) {
                console.error('❌ 매칭 WebSocket 해제 오류:', error);
            }
        }
        this.stompClient = null;
        this.messageHandlers.clear();
    }

    // 연결 상태 확인
    isConnected() {
        return this.stompClient && this.stompClient.connected;
    }

    // 메시지 전송
    send(destination, data) {
        if (this.isConnected()) {
            this.stompClient.send(destination, {}, JSON.stringify(data));
        } else {
            console.warn('매칭 WebSocket이 연결되지 않음');
        }
    }
}