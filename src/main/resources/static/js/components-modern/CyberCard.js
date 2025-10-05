// CyberCard.js - 사이버펑크 스타일 카드 컴포넌트
export default {
    name: 'CyberCard',
    props: {
        title: {
            type: String,
            default: ''
        },
        titleIcon: {
            type: String,
            default: ''
        },
        glowColor: {
            type: String,
            default: 'cyan',
            validator: (value) => ['cyan', 'purple', 'yellow', 'green', 'red', 'orange'].includes(value)
        },
        hudFrame: {
            type: Boolean,
            default: false
        },
        glassDark: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        glowColorClass() {
            const colorMap = {
                'cyan': 'from-cyan-500/10 via-cyan-400/10 to-cyan-500/10',
                'purple': 'from-purple-500/10 via-purple-400/10 to-purple-500/10',
                'yellow': 'from-yellow-500/10 via-yellow-400/10 to-yellow-500/10',
                'green': 'from-green-500/10 via-green-400/10 to-green-500/10',
                'red': 'from-red-500/10 via-red-400/10 to-red-500/10',
                'orange': 'from-orange-500/10 via-orange-400/10 to-orange-500/10'
            };
            return colorMap[this.glowColor];
        },
        titleColorClass() {
            const colorMap = {
                'cyan': 'text-cyan-400',
                'purple': 'text-purple-400',
                'yellow': 'text-yellow-400',
                'green': 'text-green-400',
                'red': 'text-red-400',
                'orange': 'text-orange-400'
            };
            return colorMap[this.glowColor];
        }
    },
    template: `
        <div :class="hudFrame ? 'hud-frame' : ''">
            <div :class="[glassDark ? 'glass-dark' : 'glass', 'rounded-2xl p-6 relative overflow-hidden']">
                <!-- 홀로그램 배경 효과 -->
                <div class="absolute inset-0 bg-gradient-to-r" :class="glowColorClass"></div>
                
                <!-- 스캔 라인 효과 -->
                <div class="absolute inset-0 opacity-10"
                     style="background: linear-gradient(180deg, transparent 30%, rgba(255,255,255,0.05) 50%, transparent 70%);
                            animation: hologram-scan 3s linear infinite;">
                </div>
                
                <!-- 컨텐츠 -->
                <div class="relative">
                    <div v-if="title" class="flex items-center gap-3 mb-4">
                        <span v-if="titleIcon" class="text-2xl neon-text">{{ titleIcon }}</span>
                        <h3 class="text-xl font-black uppercase tracking-wider" :class="titleColorClass">
                            {{ title }}
                        </h3>
                        <div class="flex-1 h-px bg-gradient-to-r from-white/20 to-transparent"></div>
                    </div>
                    
                    <slot></slot>
                </div>
            </div>
        </div>
    `
};