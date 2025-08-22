// ChatInterface.js - 채팅 인터페이스 컴포넌트
import CyberButton from './CyberButton.js';

const { nextTick } = Vue;

export default {
    name: 'ChatInterface',
    components: {
        CyberButton
    },
    props: {
        title: String,
        titleColor: {
            type: String,
            default: 'cyan'
        },
        messages: Array,
        inputPlaceholder: String,
        sendButtonText: {
            type: String,
            default: '전송'
        },
        showAiButton: {
            type: Boolean,
            default: false
        },
        panelId: String,
        onlineCount: Number
    },
    emits: ['send-message', 'request-ai'],
    data() {
        return {
            inputText: ''
        };
    },
    methods: {
        sendMessage() {
            if (this.inputText.trim()) {
                this.$emit('send-message', this.inputText);
                this.inputText = '';
            }
        },
        getMessageClass(type) {
            const classes = {
                'user': 'ml-auto bg-cyan-500/10 border-cyan-500/30',
                'ai': 'bg-purple-500/10 border-purple-500/30',
                'system': 'bg-gray-500/10 border-gray-500/30 mx-auto text-center',
                'other': 'bg-gray-500/10 border-gray-500/30'
            };
            return classes[type] || 'bg-gray-500/10 border-gray-500/30';
        }
    },
    watch: {
        messages: {
            handler() {
                nextTick(() => {
                    const container = document.getElementById(this.panelId);
                    if (container) {
                        container.scrollTop = container.scrollHeight;
                    }
                });
            },
            deep: true
        }
    },
    template: `
        <div class="glass rounded-2xl p-6">
            <div class="flex items-center justify-between mb-4">
                <h3 class="text-xl font-bold" :class="'text-' + titleColor + '-400'">
                    {{ title }}
                </h3>
                <div class="flex items-center gap-2">
                    <div class="status-online"></div>
                    <span v-if="onlineCount" class="text-xs text-gray-400">
                        {{ onlineCount }}명 온라인
                    </span>
                </div>
            </div>
            
            <div class="bg-black/50 rounded-xl h-96 overflow-y-auto p-4 mb-4 custom-scrollbar"
                 :id="panelId">
                <transition-group name="slide" tag="div">
                    <div v-for="msg in messages" 
                         :key="msg.id"
                         class="mb-3 p-3 rounded-lg glass"
                         :class="getMessageClass(msg.type)">
                        <div v-html="msg.content"></div>
                    </div>
                </transition-group>
            </div>
            
            <div class="flex gap-2">
                <input v-model="inputText"
                       @keydown.enter="sendMessage"
                       class="flex-1 px-4 py-3 bg-black/50 border rounded-lg 
                              focus:outline-none text-white font-mono"
                       :class="'border-' + titleColor + '-500/30 focus:border-' + titleColor + '-400'"
                       :placeholder="inputPlaceholder">
                <cyber-button 
                    @click="sendMessage"
                    :variant="titleColor"
                    size="md">
                    {{ sendButtonText }}
                </cyber-button>
                <cyber-button 
                    v-if="showAiButton"
                    @click="$emit('request-ai')"
                    variant="purple"
                    size="md">
                    AI
                </cyber-button>
            </div>
        </div>
        
        <style scoped>
        .slide-enter-active, .slide-leave-active {
            transition: all 0.3s ease;
        }
        .slide-enter-from {
            opacity: 0;
            transform: translateY(10px);
        }
        .slide-leave-to {
            opacity: 0;
            transform: translateY(-10px);
        }
        </style>
    `
};