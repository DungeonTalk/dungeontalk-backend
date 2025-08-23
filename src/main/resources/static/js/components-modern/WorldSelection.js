// WorldSelection.js - 세계관 선택 컴포넌트
import CyberButton from './CyberButton.js';

export default {
    name: 'WorldSelection',
    components: {
        CyberButton
    },
    props: {
        selectedWorld: String,
        isMatching: Boolean,
        matchingStatus: String,
        queueInfo: String
    },
    emits: ['select-world', 'start-matching', 'cancel-matching'],
    data() {
        return {
            worlds: [
                {
                    code: 'FANTASY',
                    name: '판타지 왕국',
                    icon: '🏰',
                    color: 'yellow',
                    features: [
                        '마법과 검',
                        '드래곤과 던전',
                        '에픽 퀘스트'
                    ]
                },
                {
                    code: 'ZOMBIE',
                    name: '아포칼립스 존',
                    icon: '🧟',
                    color: 'red',
                    features: [
                        '생존 호러',
                        '자원 관리',
                        '팀 전술'
                    ]
                }
            ]
        };
    },
    template: `
        <div class="mb-8">
            <div class="glass rounded-2xl p-6">
                <h2 class="text-2xl font-bold mb-6">
                    <span class="bg-gradient-to-r from-yellow-400 to-orange-600 bg-clip-text text-transparent">
                        세계관 선택
                    </span>
                </h2>
                
                <div class="grid md:grid-cols-2 gap-6">
                    <!-- 판타지 월드 -->
                    <div v-for="world in worlds"
                         :key="world.code"
                         @click="$emit('select-world', world.code)"
                         class="world-card glass rounded-xl p-6 cursor-pointer border-2 transition-all"
                         :class="selectedWorld === world.code ? 
                                 'border-' + world.color + '-400 bg-' + world.color + '-400/10' : 
                                 'border-white/10'">
                        <div class="flex items-start gap-4">
                            <div class="text-6xl">{{ world.icon }}</div>
                            <div class="flex-1">
                                <h3 class="text-xl font-bold mb-2"
                                    :class="'text-' + world.color + '-400'">
                                    {{ world.name }}
                                </h3>
                                <ul class="space-y-1 text-gray-300 text-sm">
                                    <li v-for="feature in world.features"
                                        :key="feature"
                                        class="flex items-center gap-2">
                                        <span :class="'text-' + world.color + '-500'">▸</span> 
                                        {{ feature }}
                                    </li>
                                </ul>
                            </div>
                        </div>
                        <div v-if="selectedWorld === world.code" 
                             class="energy-bar mt-4"></div>
                    </div>
                </div>
                
                <!-- 매칭 버튼 -->
                <div class="mt-8 text-center">
                    <cyber-button 
                        v-if="!isMatching && selectedWorld"
                        @click="$emit('start-matching')"
                        variant="green"
                        size="xl">
                        <span class="flex items-center gap-3">
                            <svg class="w-6 h-6 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                      d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                            매칭 시작
                        </span>
                    </cyber-button>
                    
                    <cyber-button 
                        v-if="isMatching"
                        @click="$emit('cancel-matching')"
                        variant="red"
                        size="xl">
                        <span class="flex items-center gap-3">
                            <svg class="w-6 h-6 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            매칭 취소
                        </span>
                    </cyber-button>
                </div>
                
                <!-- 매칭 상태 -->
                <div v-if="isMatching" class="mt-6">
                    <div class="glass-dark rounded-xl p-4 text-center">
                        <div class="text-green-400 font-mono mb-2">{{ matchingStatus }}</div>
                        <div class="text-gray-400 text-sm">{{ queueInfo }}</div>
                        <div class="energy-bar mt-3"></div>
                    </div>
                </div>
            </div>
        </div>
    `
};