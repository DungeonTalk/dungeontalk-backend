// LoginPanel.js - 로그인 패널 컴포넌트
import CyberButton from './CyberButton.js';

export default {
    name: 'LoginPanel',
    components: {
        CyberButton
    },
    props: {
        isLoggedIn: Boolean
    },
    emits: ['register', 'login'],
    data() {
        return {
            loginForm: {
                userId: '',
                password: ''
            }
        };
    },
    template: `
        <div v-if="!isLoggedIn" class="max-w-md mx-auto mb-8">
            <div class="glass rounded-2xl p-8 relative overflow-hidden">
                <div class="absolute inset-0 bg-gradient-to-br from-cyan-500/5 to-purple-500/5"></div>
                
                <div class="relative">
                    <h2 class="text-3xl font-bold mb-6 text-center">
                        <span class="bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                            던전 입장
                        </span>
                    </h2>
                    
                    <div class="space-y-4">
                        <div>
                            <label class="block text-cyan-400 text-sm font-mono mb-2">사용자명</label>
                            <input v-model="loginForm.userId"
                                   type="text"
                                   class="w-full px-4 py-3 bg-black/50 border border-cyan-500/30 rounded-lg 
                                          focus:border-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/20
                                          text-white placeholder-gray-500 font-mono"
                                   placeholder="사용자명을 입력하세요">
                        </div>
                        
                        <div>
                            <label class="block text-cyan-400 text-sm font-mono mb-2">비밀번호</label>
                            <input v-model="loginForm.password"
                                   type="password"
                                   class="w-full px-4 py-3 bg-black/50 border border-cyan-500/30 rounded-lg 
                                          focus:border-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-400/20
                                          text-white placeholder-gray-500 font-mono"
                                   placeholder="비밀번호를 입력하세요">
                        </div>
                        
                        <div class="grid grid-cols-2 gap-4 mt-6">
                            <cyber-button 
                                @click="$emit('register', loginForm)"
                                variant="purple"
                                full-width>
                                회원가입
                            </cyber-button>
                            <cyber-button 
                                @click="$emit('login', loginForm)"
                                variant="cyan"
                                full-width>
                                로그인
                            </cyber-button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `
};