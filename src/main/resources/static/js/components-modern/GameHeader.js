// GameHeader.js - 게임 헤더 컴포넌트
import CyberButton from './CyberButton.js';

export default {
    name: 'GameHeader',
    components: {
        CyberButton
    },
    props: {
        isLoggedIn: Boolean,
        currentUser: Object
    },
    emits: ['show-character', 'logout'],
    template: `
        <header class="mb-10">
            <div class="glass rounded-2xl p-6 relative overflow-hidden">
                <div class="absolute inset-0 bg-gradient-to-r from-cyan-500/10 via-purple-500/10 to-pink-500/10"></div>
                
                <div class="relative flex items-center justify-between">
                    <div class="flex items-center gap-6">
                        <div class="w-20 h-20 rounded-xl bg-gradient-to-br from-cyan-500 to-purple-600 p-0.5">
                            <div class="w-full h-full rounded-xl bg-black flex items-center justify-center">
                                <span class="text-4xl">⚔️</span>
                            </div>
                        </div>
                        
                        <div>
                            <h1 class="text-4xl font-black font-orbitron glitch" data-text="던전톡">
                                <span class="bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                                    던전톡
                                </span>
                            </h1>
                            <p class="text-cyan-400 text-sm tracking-widest font-mono mt-1">
                                차세대 RPG 경험
                            </p>
                        </div>
                    </div>
                    
                    <div class="flex items-center gap-4">
                        <!-- 사용자 정보 -->
                        <div v-if="isLoggedIn" class="glass rounded-xl px-4 py-2 flex items-center gap-3">
                            <div class="status-online"></div>
                            <span class="text-cyan-400 font-mono text-sm">
                                {{ currentUser?.name || '플레이어' }}
                            </span>
                        </div>
                        
                        <!-- 액션 버튼 -->
                        <cyber-button 
                            v-if="isLoggedIn"
                            @click="$emit('show-character')"
                            variant="purple"
                            size="md">
                            <span class="flex items-center gap-2">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                          d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                </svg>
                                캐릭터
                            </span>
                        </cyber-button>
                        
                        <cyber-button 
                            v-if="isLoggedIn"
                            @click="$emit('logout')"
                            variant="red"
                            size="md">
                            <span class="flex items-center gap-2">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                          d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                </svg>
                                로그아웃
                            </span>
                        </cyber-button>
                    </div>
                </div>
                
                <div class="energy-bar mt-4" style="--progress: 75%"></div>
            </div>
        </header>
    `
};