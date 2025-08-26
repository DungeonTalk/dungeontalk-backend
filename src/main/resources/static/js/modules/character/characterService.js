// 캐릭터 관리 서비스
export class CharacterService {
    constructor(auth) {
        this.auth = auth;
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
            
            return data.data; // boolean 값 반환
        } catch (error) {
            console.error('캐릭터 존재 여부 체크 오류:', error);
            throw error;
        }
    }

    // 종족 목록 로드
    async loadRaces() {
        try {
            const response = await fetch('/v1/characters/races', {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + this.auth.getAuthToken(),
                    'Cache-Control': 'no-cache'
                }
            });
            
            if (!response.ok) {
                throw new Error('종족 목록 조회 실패: ' + response.status);
            }
            
            const data = await response.json();
            console.log('종족 목록 조회 성공:', data);
            
            if (data.data && Array.isArray(data.data)) {
                return { success: true, races: data.data };
            } else {
                throw new Error('종족 데이터 형식이 올바르지 않습니다.');
            }
        } catch (error) {
            console.error('종족 목록 로드 오류:', error);
            throw error;
        }
    }

    // 캐릭터 생성
    async createCharacter(selectedRace) {
        if (!selectedRace) {
            throw new Error('종족을 선택해주세요.');
        }
        
        try {
            const currentUser = this.auth.getCurrentUser();
            console.log('캐릭터 생성 API 호출:', { memberId: currentUser.id, raceId: selectedRace });
            
            const response = await fetch('/v1/characters', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + this.auth.getAuthToken(),
                    'Cache-Control': 'no-cache'
                },
                body: JSON.stringify({
                    memberId: currentUser.id,
                    raceId: selectedRace
                })
            });
            
            if (!response.ok) {
                throw new Error('캐릭터 생성 실패: ' + response.status);
            }
            
            const data = await response.json();
            console.log('캐릭터 생성 성공:', data);
            
            return {
                success: true,
                message: `${selectedRace} 캐릭터가 생성되었습니다! 이제 세계관을 선택하고 게임을 시작할 수 있습니다.`,
                data: data.data
            };
            
        } catch (error) {
            console.error('캐릭터 생성 오류:', error);
            throw new Error('캐릭터 생성 중 오류가 발생했습니다: ' + error.message);
        }
    }

    // 내 캐릭터 기본 정보 조회
    async getMyCharacterBasic(memberId) {
        try {
            const response = await fetch(`/v1/characters?memberId=${memberId}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + this.auth.getAuthToken(),
                    'Cache-Control': 'no-cache'
                }
            });
            
            if (!response.ok) {
                throw new Error('캐릭터 기본 정보 조회 실패: ' + response.status);
            }
            
            const data = await response.json();
            if (data.resultCode === '200' && data.data) {
                return { success: true, character: data.data };
            } else {
                throw new Error('캐릭터 기본 정보 응답 오류');
            }
        } catch (error) {
            console.error('캐릭터 기본 정보 조회 오류:', error);
            throw error;
        }
    }

    // 캐릭터 상세 정보 조회 (계산된 스탯 포함)
    async getCharacterDetail(characterId) {
        try {
            const response = await fetch(`/v1/characters/${characterId}`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Bearer ' + this.auth.getAuthToken(),
                    'Cache-Control': 'no-cache'
                }
            });
            
            if (!response.ok) {
                throw new Error('캐릭터 상세 정보 조회 실패: ' + response.status);
            }
            
            const data = await response.json();
            if (data.resultCode === '200' && data.data) {
                return { success: true, characterDetail: data.data };
            } else {
                throw new Error('캐릭터 상세 정보 응답 오류');
            }
        } catch (error) {
            console.error('캐릭터 상세 정보 조회 오류:', error);
            throw error;
        }
    }

    // 내 캐릭터 전체 정보 로드 (기본 + 상세)
    async loadMyCharacterDetails() {
        try {
            const currentUser = this.auth.getCurrentUser();
            
            // 기본 캐릭터 정보 조회
            const basicResult = await this.getMyCharacterBasic(currentUser.id);
            const character = basicResult.character;
            
            // 캐릭터 상세 정보 조회 (계산된 스탯 포함)
            const detailResult = await this.getCharacterDetail(character.id);
            const characterDetail = detailResult.characterDetail;
            
            return {
                success: true,
                basicInfo: character,
                detailInfo: characterDetail
            };
            
        } catch (error) {
            console.error('캐릭터 전체 정보 로드 오류:', error);
            throw error;
        }
    }

    // 캐릭터 스탯 조회 (게임용)
    async fetchCharacterStats(memberId) {
        try {
            console.log('🎮 캐릭터 스탯 조회 시작 (멤버ID):', memberId);
            
            // 1단계: 멤버ID로 캐릭터 기본 정보 조회
            const basicResult = await this.getMyCharacterBasic(memberId);
            const characterId = basicResult.character.id;
            console.log('✅ 캐릭터 기본 정보 조회 성공, 캐릭터 ID:', characterId);
            
            // 2단계: 캐릭터ID로 상세 정보 조회 (계산된 스탯 포함)
            const detailResult = await this.getCharacterDetail(characterId);
            console.log('✅ 캐릭터 상세 스탯 조회 성공:', detailResult.characterDetail);
            
            return detailResult.characterDetail;
            
        } catch (error) {
            console.error('❌ 캐릭터 스탯 조회 오류:', error);
            return null;
        }
    }

    // 종족 설명 정보
    getRaceDescriptions() {
        return {
            '엘프': {
                title: '🧝‍♂️ 엘프',
                content: '마법에 뛰어난 우아한 종족입니다. 높은 마나와 마법 공격력을 가지고 있으며, 회피율도 뛰어납니다. 하지만 물리적인 체력은 상대적으로 낮습니다.'
            },
            '인간': {
                title: '👨‍🦰 인간',
                content: '균형잡힌 능력을 가진 다재다능한 종족입니다. 모든 능력치가 고르게 분포되어 있어 다양한 전투 상황에 적응할 수 있습니다.'
            },
            '드워프': {
                title: '🧔 드워프',
                content: '강인한 체력과 강력한 물리 공격력을 자랑하는 전사 종족입니다. 높은 HP와 물리 공격력을 가지고 있지만, 마법 능력과 회피율은 낮습니다.'
            }
        };
    }

    // 계산된 스탯 표시명 매핑
    getStatDisplayNames() {
        return {
            'hp': 'HP',
            'mp': 'MP',
            'physicalAttack': '물리 공격력',
            'magicAttack': '마법 공격력',
            'evasionRate': '회피율',
            'accuracy': '명중률',
            'diceOdds': '주사위 확률'
        };
    }
}