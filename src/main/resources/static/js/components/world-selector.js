export default {
    template: `
        <div class="mb-10">
            <h2 class="text-3xl font-bold text-center mb-8">세계관 선택</h2>
            
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6" v-if="worldTypes.length > 0">
                <!-- 동적 세계관 카드 -->
                <div 
                    v-for="worldType in worldTypes.filter(w => w.isActive !== false)"
                    :key="worldType.id"
                    @click="selectWorld(worldType.code)"
                    class="world-card"
                    :class="{'selected': selectedWorld === worldType.code}"
                >
                    <div class="icon-container">
                        <span>{{ getWorldEmoji(worldType.code) }}</span>
                    </div>
                    <h3 class="text-xl font-bold mb-3 text-center">{{ worldType.displayName || worldType.name }}</h3>
                    <p class="text-light-darker text-sm text-center mb-4 whitespace-pre-line">{{ worldType.description }}</p>
                    <div class="flex justify-center gap-2">
                        <span v-for="tag in getWorldTags(worldType.code)" :key="tag" class="status-badge">
                            <span>{{ tag }}</span>
                        </span>
                    </div>
                </div>
            </div>
            
            <!-- 로딩 상태 -->
            <div v-else class="text-center py-10">
                <div class="loading-dots mx-auto mb-4">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <p class="text-light-darker">세계관을 불러오는 중...</p>
            </div>
            
            <!-- 매칭 버튼 -->
            <div v-if="selectedWorld" class="text-center mt-10">
                <button 
                    @click="startMatching" 
                    class="neo-btn text-lg px-10 py-4 bg-gradient-to-r from-accent to-orange-600"
                >
                    <span class="flex items-center gap-3">
                        <span>🎮</span>
                        <span>매칭 시작</span>
                    </span>
                </button>
            </div>
        </div>
    `,
    props: {
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
            selectedWorld: null,
            worldTypes: []
        };
    },
    mounted() {
        this.loadWorldTypes();
    },
    methods: {
        async loadWorldTypes() {
            try {
                const response = await fetch('/v1/world-types');
                
                if (response.ok) {
                    const result = await response.json();
                    this.worldTypes = result;
                    console.log('세계관 리스트 로드 성공:', this.worldTypes);
                } else {
                    console.error('세계관 리스트 로드 실패:', response.status);
                    this.setDefaultWorldTypes();
                }
            } catch (error) {
                console.error('세계관 리스트 로드 오류:', error);
                this.setDefaultWorldTypes();
            }
        },
        
        setDefaultWorldTypes() {
            this.worldTypes = [
                {
                    id: 1,
                    code: 'FANTASY',
                    name: 'FANTASY',
                    displayName: '판타지 왕국',
                    description: '마법과 검의 전설적 모험\\n드래곤, 엘프, 드워프들과 함께\\n고대의 던전과 마법 아이템들',
                    isActive: true
                },
                {
                    id: 2,
                    code: 'ZOMBIE',
                    name: 'ZOMBIE',
                    displayName: '좀비 아포칼립스',
                    description: '생존을 위한 치열한 전투\\n폐허가 된 도시와 캠프\\n의료품과 무기를 찾아서',
                    isActive: true
                }
            ];
        },
        
        selectWorld(worldCode) {
            this.selectedWorld = worldCode;
            this.$emit('world-selected', {
                code: worldCode,
                name: this.getWorldName(worldCode)
            });
        },
        
        getWorldName(worldCode) {
            if (!worldCode) return '선택 안됨';
            
            const worldType = this.worldTypes.find(w => w.code === worldCode);
            if (worldType) {
                let displayName = worldType.displayName;
                if (!displayName) {
                    if (worldType.name === worldCode) {
                        const defaultNames = {
                            'FANTASY': '판타지',
                            'ZOMBIE': '좀비',
                            'SF': 'SF',
                            'MODERN': '현대',
                            'HORROR': '호러',
                            'MYSTERY': '미스터리'
                        };
                        displayName = defaultNames[worldCode] || worldType.name;
                    } else {
                        displayName = worldType.name;
                    }
                }
                return this.getWorldEmoji(worldCode) + ' ' + displayName;
            }
            return worldCode;
        },
        
        getWorldEmoji(worldCode) {
            const emojis = {
                'FANTASY': '⚔️',
                'ZOMBIE': '🧟',
                'SF': '🚀',
                'MODERN': '🏙️',
                'HORROR': '👻',
                'MYSTERY': '🔍'
            };
            return emojis[worldCode] || '🎮';
        },
        
        getWorldTags(worldCode) {
            const tags = {
                'FANTASY': ['검술', '마법', '탐험'],
                'ZOMBIE': ['생존', '전투', '탐색'],
                'SF': ['기술', '우주', '미래'],
                'MODERN': ['현실', '도시', '추리'],
                'HORROR': ['공포', '스릴', '미스터리'],
                'MYSTERY': ['추리', '수사', '퍼즐']
            };
            return tags[worldCode] || ['모험', '탐험', '스토리'];
        },
        
        async startMatching() {
            if (!this.selectedWorld) {
                alert('세계관을 선택해주세요.');
                return;
            }
            
            // 캐릭터 존재 여부 체크
            const hasCharacter = await this.checkCharacter();
            if (!hasCharacter) {
                alert('캐릭터가 없습니다. 먼저 "⚔️ 내 캐릭터" 버튼을 클릭하여 캐릭터를 생성해주세요.');
                return;
            }
            
            this.$emit('start-matching', this.selectedWorld);
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
                    return !!result.data;
                }
                return false;
            } catch (error) {
                console.error('캐릭터 체크 실패:', error);
                return false;
            }
        }
    }
};