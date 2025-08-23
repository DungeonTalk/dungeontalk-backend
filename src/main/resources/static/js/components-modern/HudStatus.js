// HudStatus.js - HUD 스타일 상태 표시 컴포넌트
export default {
    name: 'HudStatus',
    props: {
        label: {
            type: String,
            required: true
        },
        value: {
            type: String,
            required: true
        },
        color: {
            type: String,
            default: 'cyan'
        },
        progress: {
            type: Number,
            default: null
        },
        showRadar: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        labelColorClass() {
            const colorMap = {
                'cyan': 'text-cyan-400',
                'purple': 'text-purple-400',
                'yellow': 'text-yellow-400',
                'green': 'text-green-400',
                'red': 'text-red-400',
                'orange': 'text-orange-400'
            };
            return colorMap[this.color];
        }
    },
    template: `
        <div class="hud-frame">
            <div class="glass-dark rounded-lg p-4">
                <div class="flex items-center justify-between">
                    <div class="flex-1">
                        <div class="text-xs font-mono mb-1" :class="labelColorClass">
                            {{ label }}
                        </div>
                        <div class="text-2xl font-bold text-white">
                            {{ value }}
                        </div>
                        <div v-if="progress !== null" 
                             class="energy-bar mt-2" 
                             :style="'--progress: ' + progress + '%'">
                        </div>
                    </div>
                    <div v-if="showRadar" class="radar ml-4"></div>
                </div>
            </div>
        </div>
    `
};