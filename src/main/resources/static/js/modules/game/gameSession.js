// 게임 세션 관리
export class GameSession {
    constructor(auth) {
        this.auth = auth;
        this.currentGameSession = null;
        this.aiGameRoomId = null;
        this.partyRoomId = null;
        this.gameSessionStarted = false;
        this.currentTurnNumber = 1;
        this.participantNicknames = new Set();
        this.characterStats = null;
    }

    // 게임 세션 시작
    async startGameSession(matchData) {
        try {
            // 중복 요청 방지
            if (this.gameSessionStarted) {
                console.log('🔄 게임 세션이 이미 시작됨, 중복 요청 무시');
                return;
            }
            
            // JWT 토큰 유효성 검사
            const authToken = this.auth.getAuthToken();
            if (!authToken || authToken === 'null' || authToken.trim() === '') {
                throw new Error('인증 토큰이 필요합니다. 다시 로그인해주세요.');
            }
            
            this.gameSessionStarted = true;
            this.currentGameSession = matchData;
            this.aiGameRoomId = matchData.aiGameRoomId;
            this.partyRoomId = matchData.chatRoomId;
            
            console.log('🔑 JWT 토큰 확인됨, AI 게임 세션 시작 요청:', this.aiGameRoomId);
            
            // AI 게임방 세션 시작
            const sessionResponse = await fetch(`/v1/rooms/ai/${this.aiGameRoomId}/start`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + authToken
                }
            });
            
            const sessionResult = await sessionResponse.json();
            if (sessionResponse.ok && sessionResult.resultCode === '200') {
                console.log('AI 게임 세션 시작 성공');
                
                // 캐릭터 스탯 조회 (게임 시작 시 한번만)
                await this.loadCharacterStats();
                
                return { success: true, data: sessionResult };
            } else {
                console.error('❌ AI 게임 세션 시작 실패:', {
                    status: sessionResponse.status,
                    statusText: sessionResponse.statusText,
                    result: sessionResult
                });
                
                this.gameSessionStarted = false;
                
                if (sessionResponse.status === 401) {
                    throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
                } else {
                    throw new Error('AI 게임 세션 시작 실패: ' + (sessionResult.msg || '알 수 없는 오류'));
                }
            }
        } catch (error) {
            console.error('❌ 게임 세션 시작 오류:', error);
            this.gameSessionStarted = false;
            throw error;
        }
    }

    // AI 응답 요청 (REST API 호출)
    async requestAiResponse() {
        if (!this.aiGameRoomId || !this.currentGameSession) {
            throw new Error('게임 세션이 유효하지 않습니다.');
        }
        
        const currentUser = this.auth.getCurrentUser();
        const authToken = this.auth.getAuthToken();
        
        const requestData = {
            gameId: this.currentGameSession.gameSessionId,
            currentUser: currentUser.name,
            currentMessage: "사용자 행동 완료",
            turnNumber: this.currentTurnNumber,
            gameStartTime: this.getGameStartTime() ? Math.floor(this.getGameStartTime() / 1000) : null,
            targetDuration: 15, // 15분
            characterStats: this.characterStats
        };
        
        console.log('AI 응답 요청 시작:', requestData);
        
        const response = await fetch(`/v1/rooms/ai/${this.aiGameRoomId}/ai/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + authToken
            },
            body: JSON.stringify(requestData)
        });
        
        const data = await response.json();
        console.log('AI 응답 요청 완료:', data);
        
        if (data.resultCode !== '200-1') {
            throw new Error('AI 응답 요청 실패: ' + data.msg);
        } else {
            console.log('✅ AI 응답 생성 성공');
            return data;
        }
    }

    // 캐릭터 스탯 조회 (멤버ID로 캐릭터 찾은 후 상세 정보 조회)
    async fetchCharacterStats(memberId) {
        const authToken = this.auth.getAuthToken();
        
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
    async loadCharacterStats() {
        try {
            const currentUser = this.auth.getCurrentUser();
            const memberId = currentUser.id;
            
            console.log('🎯 캐릭터 스탯 로딩 시작 (멤버ID)...', memberId);
            this.characterStats = await this.fetchCharacterStats(memberId);
            
            if (this.characterStats) {
                console.log('✅ 캐릭터 스탯 로딩 완료:', this.characterStats);
                return { success: true, stats: this.characterStats };
            } else {
                console.warn('⚠️ 캐릭터 스탯 로딩 실패 - 기본 게임으로 진행');
                return { success: false, message: '캐릭터 정보를 불러올 수 없어 기본 게임으로 진행됩니다.' };
            }
        } catch (error) {
            console.error('❌ 캐릭터 스탯 로딩 오류:', error);
            return { success: false, message: '캐릭터 정보 로딩 중 오류가 발생했습니다.' };
        }
    }

    // 게임 나가기
    async leaveGame() {
        try {
            console.log('🚪 게임 나가기 시작...');
            const authToken = this.auth.getAuthToken();
            const currentUser = this.auth.getCurrentUser();
            
            // 1. AI 게임방 퇴장
            if (this.aiGameRoomId && authToken) {
                try {
                    const aiLeaveResponse = await fetch(`/v1/aichat/rooms/${this.aiGameRoomId}/leave?participantId=${currentUser.id}`, {
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
            if (this.partyRoomId && authToken) {
                try {
                    const partyLeaveResponse = await fetch(`/v1/chat/room/${this.partyRoomId}/leave/${currentUser.id}`, {
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
            
            // 3. 게임 상태 초기화
            this.resetGameState();
            
            console.log('✅ 게임 나가기 완료');
            return { success: true };
            
        } catch (error) {
            console.error('❌ 게임 나가기 오류:', error);
            throw new Error('게임 나가기 중 오류가 발생했습니다: ' + error.message);
        }
    }

    // 게임 상태 초기화 (로그아웃하지 않음)
    resetGameState() {
        this.gameSessionStarted = false;
        this.currentGameSession = null;
        this.aiGameRoomId = null;
        this.partyRoomId = null;
        this.currentTurnNumber = 1;
        this.participantNicknames.clear();
        this.characterStats = null;
    }

    // getter methods
    getCurrentGameSession() {
        return this.currentGameSession;
    }

    getAiGameRoomId() {
        return this.aiGameRoomId;
    }

    getPartyRoomId() {
        return this.partyRoomId;
    }

    isGameSessionStarted() {
        return this.gameSessionStarted;
    }

    getCurrentTurnNumber() {
        return this.currentTurnNumber;
    }

    incrementTurnNumber() {
        this.currentTurnNumber++;
    }

    getParticipantNicknames() {
        return this.participantNicknames;
    }

    addParticipantNickname(nickname) {
        this.participantNicknames.add(nickname);
    }

    removeParticipantNickname(nickname) {
        this.participantNicknames.delete(nickname);
    }

    getCharacterStats() {
        return this.characterStats;
    }

    // 게임 시작 시간 관련 메서드 (TRPG 타이머와 연동)
    getGameStartTime() {
        // 타이머 모듈에서 관리하는 시간을 가져오는 메서드
        return window.gameStartTime || null;
    }
}