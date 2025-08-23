export default {
    template: `
        <div v-if="show" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click="close">
            <div class="glass-card p-8 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto" @click.stop>
                <div class="flex justify-between items-center mb-6">
                    <h2 class="text-2xl font-bold">⚔️ 내 캐릭터</h2>
                    <button @click="close" class="text-2xl hover:text-red-400 transition-colors">✕</button>
                </div>
                
                <!-- 캐릭터가 있는 경우 -->
                <div v-if="characterData" class="space-y-6">
                    <!-- 캐릭터 기본 정보 -->
                    <div class="glass-card p-6">
                        <h3 class="text-xl font-bold mb-4">캐릭터 정보</h3>
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <span class="text-light-darker">종족:</span>
                                <span class="ml-2 font-bold">{{ characterData.raceName || characterData.raceId }}</span>
                            </div>
                            <div>
                                <span class="text-light-darker">레벨:</span>
                                <span class="ml-2 font-bold text-accent">{{ characterData.playerLevel }}</span>
                            </div>
                            <div>
                                <span class="text-light-darker">경험치:</span>
                                <span class="ml-2 font-bold">{{ characterData.totalExp || 0 }}</span>
                            </div>
                            <div>
                                <span class="text-light-darker">미할당 포인트:</span>
                                <span class="ml-2 font-bold text-yellow-400">{{ characterData.unspentPoints || 0 }}</span>
                            </div>
                        </div>
                    </div>
                    
                    <!-- 스탯 정보 -->
                    <div class="glass-card p-6">
                        <h3 class="text-xl font-bold mb-4">📊 능력치</h3>
                        <div class="grid grid-cols-3 gap-4">
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-str);">{{ characterData.strength || 10 }}</div>
                                <div class="text-sm text-light-darker">STR (힘)</div>
                            </div>
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-wil);">{{ characterData.willpower || 10 }}</div>
                                <div class="text-sm text-light-darker">WIL (의지)</div>
                            </div>
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-int);">{{ characterData.intelligence || 10 }}</div>
                                <div class="text-sm text-light-darker">INT (지능)</div>
                            </div>
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-wis);">{{ characterData.wisdom || 10 }}</div>
                                <div class="text-sm text-light-darker">WIS (지혜)</div>
                            </div>
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-dex);">{{ characterData.dexterity || 10 }}</div>
                                <div class="text-sm text-light-darker">DEX (민첩)</div>
                            </div>
                            <div class="text-center">
                                <div class="text-2xl font-bold" style="color: var(--stat-luk);">{{ characterData.luck || 10 }}</div>
                                <div class="text-sm text-light-darker">LUK (행운)</div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 캐릭터가 없는 경우 - 생성 폼 -->
                <div v-else class="text-center space-y-6">
                    <div>
                        <h3 class="text-xl font-bold mb-3">캐릭터가 없습니다</h3>
                        <p class="text-light-darker">새로운 캐릭터를 생성하여 모험을 시작하세요!</p>
                    </div>
                    
                    <div class="glass-card p-6">
                        <h4 class="text-lg font-bold mb-4">캐릭터 생성</h4>
                        <div class="space-y-4">
                            <div>
                                <label class="block text-sm font-medium mb-2 text-light-darker">종족 선택</label>
                                <select v-model="newCharacterForm.race" class="modern-input w-full">
                                    <option value="HUMAN">인간 - 균형잡힌 능력치</option>
                                    <option value="ELF">엘프 - 높은 지능과 민첩성</option>
                                    <option value="DWARF">드워프 - 강한 체력과 힘</option>
                                    <option value="ORC">오크 - 뛰어난 전투 능력</option>
                                </select>
                            </div>
                            
                            <button @click="createCharacter" class="neo-btn w-full bg-gradient-to-r from-accent to-orange-600">
                                ⚔️ 캐릭터 생성
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    props: {
        show: {
            type: Boolean,
            default: false
        },
        authToken: {
            type: String,
            required: true
        },
        currentUser: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            characterData: null,
            newCharacterForm: {
                race: 'HUMAN'
            }
        };
    },
    watch: {
        show(newVal) {
            if (newVal) {
                this.loadCharacterData();
            }
        }
    },
    methods: {
        close() {
            this.$emit('close');
        },
        
        async checkCharacter() {
            try {
                const response = await fetch(`/v1/characters?memberId=${this.currentUser.id}`, {
                    headers: {
                        'Authorization': 'Bearer ' + this.authToken
                    }
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.data) {
                        this.characterData = result.data;
                        return true;
                    }
                }
                return false;
            } catch (error) {
                console.error('캐릭터 체크 실패:', error);
                return false;
            }
        },
        
        async loadCharacterData() {
            try {
                const hasCharacter = await this.checkCharacter();
                if (hasCharacter && this.characterData) {
                    const detailResponse = await fetch(`/v1/characters/${this.characterData.id}`, {
                        headers: {
                            'Authorization': 'Bearer ' + this.authToken
                        }
                    });
                    
                    if (detailResponse.ok) {
                        const detailResult = await detailResponse.json();
                        this.characterData = detailResult.data;
                    }
                }
            } catch (error) {
                console.error('캐릭터 데이터 로드 실패:', error);
            }
        },
        
        async createCharacter() {
            if (!this.newCharacterForm.race) {
                alert('종족을 선택해주세요.');
                return;
            }
            
            try {
                const response = await fetch('/v1/characters', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + this.authToken
                    },
                    body: JSON.stringify({
                        memberId: this.currentUser.id,
                        raceId: this.newCharacterForm.race
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.characterData = result.data;
                    alert('캐릭터가 생성되었습니다!');
                    await this.loadCharacterData();
                    this.$emit('character-created', this.characterData);
                } else {
                    const error = await response.text();
                    alert('캐릭터 생성 실패: ' + error);
                }
            } catch (error) {
                console.error('캐릭터 생성 오류:', error);
                alert('캐릭터 생성 중 오류가 발생했습니다.');
            }
        }
    }
};