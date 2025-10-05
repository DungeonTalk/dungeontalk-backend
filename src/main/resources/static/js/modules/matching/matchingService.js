// 매칭 서비스 관리
export class MatchingService {
    constructor(auth) {
        this.auth = auth;
        this.isMatching = false;
        this.selectedWorld = null;
        this.matchingStompClient = null;
    }

    // 세계관 선택
    selectWorld(worldType) {
        this.selectedWorld = worldType;
        console.log(`✨ ${this.getWorldName(worldType)} 세계관 선택됨`);
        return true;
    }

    getWorldName(worldType) {
        const names = {
            'FANTASY': '🏰 판타지',
            'ZOMBIE': '🧟 좀비'
        };
        return names[worldType] || worldType;
    }

    // 캐릭터 존재 여부 체크
    async checkCharacterExists(memberId) {
        try {
            const response = await fetch(`/v1/characters/exists?memberId=${memberId}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + this.auth.getAuthToken(),
                    'Cache-Control': 'no-cache'
                }
            });
            
            if (!response.ok) {
                throw new Error('캐릭터 존재 여부 확인 실패: ' + response.status);
            }
            
            const data = await response.json();
            console.log('캐릭터 존재 여부 체크 결과:', data);
            
            return data.data;
        } catch (error) {
            console.error('캐릭터 존재 여부 체크 오류:', error);
            throw error;
        }
    }

    // 매칭 시작
    async startMatching() {
        const authToken = this.auth.getAuthToken();
        const currentUser = this.auth.getCurrentUser();

        if (!authToken || authToken === 'null') {
            throw new Error('로그인이 필요합니다.');
        }
        
        if (!currentUser || !currentUser.id) {
            throw new Error('로그인 정보가 유효하지 않습니다.');
        }
        
        // 캐릭터 존재 여부 체크
        try {
            const hasCharacter = await this.checkCharacterExists(currentUser.id);
            if (!hasCharacter) {
                throw new Error('캐릭터가 없습니다. 먼저 "⚔️ 내 캐릭터" 버튼을 클릭하여 캐릭터를 생성해주세요.');
            }
        } catch (error) {
            console.error('캐릭터 체크 실패:', error);
            throw new Error('캐릭터 정보 확인 중 오류가 발생했습니다.');
        }
        
        try {
            console.log('=== 매칭 요청 디버그 정보 ===');
            console.log('currentUser:', currentUser);
            console.log('this.selectedWorld:', this.selectedWorld);
            console.log('authToken:', authToken ? 'EXISTS' : 'NULL');
            
            const requestData = {
                memberId: currentUser.id,
                worldTypeCode: this.selectedWorld
            };
            console.log('전송할 데이터:', requestData);
            
            const response = await fetch('/v1/match/join', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + authToken,
                    'Cache-Control': 'no-cache'
                },
                body: JSON.stringify(requestData)
            });
            
            console.log('API 응답 상태:', response.status, response.statusText);
            
            if (!response.ok) {
                let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
                try {
                    const errorBody = await response.text();
                    console.log('에러 응답 본문:', errorBody);
                    errorMessage += ` - ${errorBody}`;
                } catch (e) {
                    console.log('에러 응답 본문을 읽을 수 없습니다:', e);
                }
                throw new Error(errorMessage);
            }
            
            const result = await response.json();
            console.log('매칭 시작 API 응답:', result);
            
            if (result.resultCode === '200') {
                this.isMatching = true;
                return { success: true, data: result };
            } else {
                throw new Error(result.msg || '알 수 없는 오류');
            }
        } catch (error) {
            throw new Error('매칭 오류: ' + error.message);
        }
    }

    // 매칭 취소
    async cancelMatching() {
        const authToken = this.auth.getAuthToken();
        const currentUser = this.auth.getCurrentUser();

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
                this.isMatching = false;
                return { success: true };
            } else {
                throw new Error(result.msg || '알 수 없는 오류');
            }
        } catch (error) {
            throw new Error('매칭 취소 오류: ' + error.message);
        }
    }

    // 매칭 메시지 처리
    handleMatchingMessage(message) {
        switch (message.type) {
            case 'QUEUE_STATUS_UPDATE':
                return { type: 'queue_update', data: message.data };
            case 'MATCHING_COMPLETE':
                return { type: 'match_complete', data: message.data };
            case 'MATCHING_CANCELLED':
                this.handleMatchingCancelled();
                return { type: 'match_cancelled' };
            case 'ERROR':
                console.error('매칭 오류:', message.data);
                return { type: 'error', message: message.data };
            default:
                return { type: 'unknown', data: message };
        }
    }

    // 매칭 취소 처리
    handleMatchingCancelled() {
        this.isMatching = false;
    }

    // getter methods
    getSelectedWorld() {
        return this.selectedWorld;
    }

    getIsMatching() {
        return this.isMatching;
    }

    setMatchingStompClient(client) {
        this.matchingStompClient = client;
    }

    getMatchingStompClient() {
        return this.matchingStompClient;
    }
}