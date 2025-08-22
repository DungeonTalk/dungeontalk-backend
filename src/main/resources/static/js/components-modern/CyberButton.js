// CyberButton.js - 사이버펑크 스타일 버튼 컴포넌트
export default {
    name: 'CyberButton',
    props: {
        variant: {
            type: String,
            default: 'cyan',
            validator: (value) => ['cyan', 'purple', 'yellow', 'green', 'red', 'orange'].includes(value)
        },
        size: {
            type: String,
            default: 'md',
            validator: (value) => ['sm', 'md', 'lg', 'xl'].includes(value)
        },
        disabled: {
            type: Boolean,
            default: false
        },
        loading: {
            type: Boolean,
            default: false
        },
        fullWidth: {
            type: Boolean,
            default: false
        },
        glowing: {
            type: Boolean,
            default: true
        }
    },
    computed: {
        borderColorClass() {
            const colorMap = {
                'cyan': 'border-cyan-500',
                'purple': 'border-purple-500',
                'yellow': 'border-yellow-500',
                'green': 'border-green-500',
                'red': 'border-red-500',
                'orange': 'border-orange-500'
            };
            return colorMap[this.variant];
        },
        textColorClass() {
            const colorMap = {
                'cyan': 'text-cyan-400',
                'purple': 'text-purple-400',
                'yellow': 'text-yellow-400',
                'green': 'text-green-400',
                'red': 'text-red-400',
                'orange': 'text-orange-400'
            };
            return colorMap[this.variant];
        },
        hoverBgClass() {
            const colorMap = {
                'cyan': 'hover:bg-cyan-500/20',
                'purple': 'hover:bg-purple-500/20',
                'yellow': 'hover:bg-yellow-500/20',
                'green': 'hover:bg-green-500/20',
                'red': 'hover:bg-red-500/20',
                'orange': 'hover:bg-orange-500/20'
            };
            return colorMap[this.variant];
        },
        sizeClasses() {
            const sizeMap = {
                'sm': 'px-4 py-2 text-sm',
                'md': 'px-6 py-3 text-base',
                'lg': 'px-8 py-4 text-lg',
                'xl': 'px-10 py-5 text-xl'
            };
            return sizeMap[this.size];
        }
    },
    template: `
        <button 
            :class="[
                'cyber-btn',
                borderColorClass,
                textColorClass,
                hoverBgClass,
                sizeClasses,
                fullWidth ? 'w-full' : '',
                disabled || loading ? 'opacity-50 cursor-not-allowed' : ''
            ]"
            :disabled="disabled || loading">
            <span class="flex items-center justify-center gap-2">
                <svg v-if="loading" class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <slot></slot>
            </span>
        </button>
    `
};