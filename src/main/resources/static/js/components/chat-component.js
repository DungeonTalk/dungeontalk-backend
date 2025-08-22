export default {
    template: `
        <div class="chat-container">
            <div class="chat-header">
                <div class="flex items-center gap-2">
                    <span>{{ isAiChat ? '🎭' : '👥' }}</span>
                    <span>{{ title }}</span>
                </div>
                <span class="status-badge">
                    <span class="status-dot online"></span>
                    <span>{{ isAiChat ? 'AI' : '온라인' }}</span>
                </span>
            </div>
            
            <div class="chat-messages" ref="messagesContainer">
                <div v-for="msg in messages" :key="msg.id" 
                     class="message" 
                     :class="msg.sender === currentUserName ? 'user' : ''">
                    <div class="message-bubble" 
                         :class="getMessageClass(msg)">
                        <p v-if="msg.sender !== currentUserName && msg.type !== 'system'" 
                           class="text-xs font-semibold mb-1">{{ msg.sender }}</p>
                        <p class="text-sm">{{ msg.content }}</p>
                        <span class="text-xs opacity-70">{{ msg.time }}</span>
                    </div>
                </div>
            </div>
            
            <div class="chat-input-container">
                <input 
                    v-model="inputMessage"
                    @keyup.enter="sendMessage"
                    type="text"
                    class="modern-input flex-1"
                    :placeholder="placeholder"
                    :disabled="disabled"
                >
                <button @click="sendMessage" class="neo-btn" :disabled="disabled">
                    <span>➤</span>
                </button>
                <button v-if="isAiChat" 
                        @click="requestAiResponse" 
                        class="neo-btn bg-gradient-to-r from-magic to-magic-dark" 
                        :disabled="disabled">
                    <span>✨</span>
                </button>
            </div>
        </div>
    `,
    props: {
        title: {
            type: String,
            required: true
        },
        isAiChat: {
            type: Boolean,
            default: false
        },
        messages: {
            type: Array,
            default: () => []
        },
        placeholder: {
            type: String,
            default: '메시지 입력...'
        },
        disabled: {
            type: Boolean,
            default: false
        },
        currentUserName: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            inputMessage: ''
        };
    },
    watch: {
        messages() {
            this.$nextTick(() => {
                this.scrollToBottom();
            });
        }
    },
    methods: {
        sendMessage() {
            if (!this.inputMessage.trim() || this.disabled) return;
            
            this.$emit('send-message', this.inputMessage);
            this.inputMessage = '';
        },
        
        requestAiResponse() {
            if (this.disabled) return;
            this.$emit('request-ai-response');
        },
        
        getMessageClass(msg) {
            if (msg.type === 'system') return 'system';
            if (msg.type === 'ai') return 'ai';
            if (msg.sender === this.currentUserName || msg.type === 'user') return 'user';
            return 'other';
        },
        
        scrollToBottom() {
            if (this.$refs.messagesContainer) {
                this.$refs.messagesContainer.scrollTop = this.$refs.messagesContainer.scrollHeight;
            }
        }
    }
};