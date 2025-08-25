// 파티 채팅 관리
export class PartyChat {
    constructor(auth, gameSession) {
        this.auth = auth;
        this.gameSession = gameSession;
        this.stompClient = null;
        this.messageHandlers = new Map();
        this.lastParticipantUpdate = '';
    }

    // 파티 WebSocket 연결
    connect() {
        const partyRoomId = this.gameSession.getPartyRoomId();
        const authToken = this.auth.getAuthToken();
        
        console.log('=== 파티 WebSocket 연결 시도 ===');
        
        // 기존 파티 WebSocket 연결 해제
        if (this.stompClient && this.stompClient.connected) {
            console.log('🔄 기존 파티 WebSocket 연결 해제 중...');
            try {
                this.stompClient.disconnect();
                console.log('✅ 기존 파티 WebSocket 연결 해제 완료');
            } catch (error) {
                console.error('❌ 기존 파티 WebSocket 해제 오류:', error);
            }
        }
        
        if (!authToken) {
            console.error('파티 WebSocket 연결 실패: 인증 토큰이 없습니다.');
            return Promise.reject(new Error('인증 토큰이 없습니다.'));
        }
        
        if (!partyRoomId) {
            console.error('파티 WebSocket 연결 실패: partyRoomId가 없습니다.');
            return Promise.reject(new Error('파티룸 ID가 없습니다.'));
        }
        
        return new Promise((resolve, reject) => {
            try {
                const socket = new SockJS('/ws-chat?token=' + encodeURIComponent(authToken) + '&roomId=' + encodeURIComponent(partyRoomId));
                this.stompClient = Stomp.over(socket);
                
                this.stompClient.connect({}, (frame) => {
                    console.log('👥 파티 채팅 WebSocket 연결 성공');
                    
                    // 사용자 채팅방 구독 (단일 구독)
                    const subscription = this.stompClient.subscribe(`/sub/chat/room/${partyRoomId}`, (message) => {
                        const userMessage = JSON.parse(message.body);
                        this.handleUserMessage(userMessage);
                    });
                    
                    console.log('✅ 파티 채팅방 구독 완료:', `/sub/chat/room/${partyRoomId}`);
                    resolve();
                    
                }, (error) => {
                    console.error('❌ 파티 채팅 WebSocket 연결 실패:', error);
                    reject(error);
                });
            } catch (error) {
                console.error('❌ 파티 WebSocket 오류:', error);
                reject(error);
            }
        });
    }

    // 사용자 메시지 처리
    handleUserMessage(message) {
        const currentUser = this.auth.getCurrentUser();
        
        // 자신이 보낸 메시지는 이미 화면에 표시했으므로 무시
        if (message.senderId === currentUser.id) {
            return;
        }
        
        if (message.type === 'TALK') {
            const nickname = message.senderNickname || message.senderId || 'Unknown';
            
            // 닉네임 수집 (자신 제외)
            if (nickname !== 'Unknown' && nickname !== currentUser.name) {
                this.gameSession.addParticipantNickname(nickname);
                this.updateParticipantsList();
            }
            
            this.notifyHandlers('userMessage', {
                type: 'other',
                content: `${nickname}: ${message.content}`,
                nickname: nickname,
                senderId: message.senderId
            });
            
        } else if (message.type === 'ENTER') {
            // 입장 메시지에서도 닉네임 추출
            const nickname = this.extractNicknameFromEnterMessage(message.content);
            if (nickname && nickname !== currentUser.name) {
                this.gameSession.addParticipantNickname(nickname);
                this.updateParticipantsList();
            }
            
            this.notifyHandlers('systemMessage', {
                type: 'system',
                content: message.content
            });
            
        } else if (message.type === 'LEAVE') {
            // 퇴장 메시지에서 닉네임 제거
            const nickname = this.extractNicknameFromLeaveMessage(message.content);
            if (nickname) {
                this.gameSession.removeParticipantNickname(nickname);
                this.updateParticipantsList();
            }
            
            this.notifyHandlers('systemMessage', {
                type: 'system',
                content: message.content
            });
        }
    }

    // 메시지 전송
    sendMessage(message) {
        const partyRoomId = this.gameSession.getPartyRoomId();
        const currentUser = this.auth.getCurrentUser();
        
        if (!message.trim()) return;
        
        // WebSocket으로 전송
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send('/pub/room/chat/send', {}, JSON.stringify({
                roomId: partyRoomId,
                roomType: 'PLAYER_CHAT',
                senderId: currentUser.id,
                messageType: 'USER',
                content: message,
                chatRoomId: partyRoomId,
                senderNickname: currentUser.name
            }));
            
            // 내 메시지 표시를 위해 핸들러에 알림
            this.notifyHandlers('myMessage', { content: message });
        } else {
            console.warn('파티 WebSocket이 연결되지 않음');
        }
    }

    // 퇴장 메시지 전송
    sendLeaveMessage() {
        const partyRoomId = this.gameSession.getPartyRoomId();
        const currentUser = this.auth.getCurrentUser();
        
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.send('/pub/room/chat/send', {}, JSON.stringify({
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
    }

    // 입장 메시지에서 닉네임 추출
    extractNicknameFromEnterMessage(content) {
        const match = content.match(/(.+)님이?\\s*입장했습니다?/);
        return match ? match[1].trim() : null;
    }

    // 퇴장 메시지에서 닉네임 추출
    extractNicknameFromLeaveMessage(content) {
        const match = content.match(/(.+)님이?\\s*퇴장했습니다?/);
        return match ? match[1].trim() : null;
    }

    // 참여자 목록 업데이트 (중복 방지)
    updateParticipantsList() {
        const currentUser = this.auth.getCurrentUser();
        const participantNicknames = this.gameSession.getParticipantNicknames();
        const participantNames = Array.from(participantNicknames);
        
        if (participantNames.length > 0) {
            // 자신 포함해서 표시
            const allParticipants = [currentUser.name, ...participantNames];
            const currentList = allParticipants.join(', ');
            
            // 이전과 다를 때만 업데이트
            if (currentList !== this.lastParticipantUpdate) {
                this.notifyHandlers('participantUpdate', {
                    type: 'system',
                    content: `👥 현재 모험가: ${currentList} (총 ${allParticipants.length}명)`,
                    participants: allParticipants,
                    count: allParticipants.length
                });
                this.lastParticipantUpdate = currentList;
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
                console.error('파티 채팅 핸들러 오류:', error);
            }
        });
    }

    // 연결 해제
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.disconnect();
                console.log('✅ 파티 WebSocket 연결 해제');
            } catch (error) {
                console.error('❌ 파티 WebSocket 해제 오류:', error);
            }
        }
        this.stompClient = null;
        this.messageHandlers.clear();
        this.lastParticipantUpdate = '';
    }

    // 연결 상태 확인
    isConnected() {
        return this.stompClient && this.stompClient.connected;
    }
}