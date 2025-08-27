// 메시지 UI 처리 및 출력 관리
export class MessageHandler {
    constructor() {
        this.gameEndHandlers = [];
    }

    // AI 메시지 추가
    addAiMessage(type, content) {
        const messagesDiv = document.getElementById('aiMessages');
        if (!messagesDiv) {
            console.warn('aiMessages 엘리먼트를 찾을 수 없습니다.');
            return;
        }

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        
        // HTML 이스케이프 후 줄바꿈 및 포맷팅 처리
        const escapedContent = this.escapeHtml(content);
        const formattedContent = this.formatMessage(escapedContent);
        
        messageDiv.innerHTML = formattedContent;
        messagesDiv.appendChild(messageDiv);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    // 오른쪽 사용자 메시지 추가
    addRightUserMessage(type, content) {
        const messagesDiv = document.getElementById('rightUserMessages');
        if (!messagesDiv) {
            console.warn('rightUserMessages 엘리먼트를 찾을 수 없습니다.');
            return;
        }

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        
        // HTML 이스케이프 후 줄바꿈 및 포맷팅 처리
        const escapedContent = this.escapeHtml(content);
        const formattedContent = this.formatMessage(escapedContent);
        
        messageDiv.innerHTML = formattedContent;
        messagesDiv.appendChild(messageDiv);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    // HTML 이스케이프
    escapeHtml(content) {
        return content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;');
    }

    // 메시지 포맷팅
    formatMessage(escapedContent) {
        return escapedContent
            .replace(/\n/g, '<br>')           // \\n을 <br>로 변환
            .replace(/\r\n/g, '<br>')        // \\r\\n을 <br>로 변환
            .replace(/\*{3,}/g, '<span style="color: #666; font-style: italic;">[BLOCKED]</span>')  // ***를 [BLOCKED]로 변환
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **텍스트**를 굵게
            .replace(/\*(.*?)\*/g, '<em>$1</em>')            // *텍스트*를 기울임
            .replace(/`(.*?)`/g, '<code>$1</code>');         // `코드`를 코드 스타일
    }

    // 게임 종료 메시지 처리
    handleGameEndMessage(message) {
        console.log('🎮 게임 종료 메시지 수신:', message);
        
        // 게임 결과 분석
        const gameResult = this.analyzeGameResult(message.content);
        
        // 게임 종료 메시지 표시
        this.addAiMessage('system', this.getGameEndSystemMessage(gameResult));
        if (message.content && message.content.trim()) {
            this.addAiMessage('ai', message.content);
        }
        
        // 상태 업데이트
        this.updateGameEndUI();
        
        // 게임 종료 배너 표시
        this.showGameEndBanner(gameResult);
        
        // 게임 종료 효과 추가
        this.showGameEndEffect(message.content);
        
        // 게임 종료 핸들러 실행
        this.notifyGameEndHandlers(gameResult, message.content);
    }

    // 게임 결과 분석
    analyzeGameResult(content) {
        if (!content) return 'UNKNOWN';
        
        const lowerContent = content.toLowerCase();
        
        // 성공 키워드들
        const successKeywords = [
            '성공', '승리', '완료', '클리어', '달성', '해결', '구출', '탈출',
            'success', 'victory', 'complete', 'clear', 'achieve', 'win'
        ];
        
        // 실패 키워드들
        const failureKeywords = [
            '실패', '패배', '전멸', '게임오버', '죽었', '쓰러졌', '불가능',
            'failure', 'defeat', 'dead', 'died', 'game over', 'impossible'
        ];
        
        // 시간 초과 키워드들
        const timeoutKeywords = [
            '시간', '초과', '타임', '종료', 'time', 'timeout', 'expired'
        ];
        
        // 키워드 우선순위로 판단
        if (successKeywords.some(keyword => lowerContent.includes(keyword))) {
            return 'SUCCESS';
        } else if (failureKeywords.some(keyword => lowerContent.includes(keyword))) {
            return 'FAILURE';
        } else if (timeoutKeywords.some(keyword => lowerContent.includes(keyword))) {
            return 'TIMEOUT';
        }
        
        return 'UNKNOWN';
    }

    // 게임 종료 시스템 메시지 생성
    getGameEndSystemMessage(gameResult) {
        const messages = {
            'SUCCESS': '🎉 축하합니다! 게임이 성공적으로 클리어되었습니다!',
            'FAILURE': '💀 게임이 종료되었습니다. 아쉽지만 다음 기회에 도전해보세요!',
            'TIMEOUT': '⏰ 시간이 초과되어 게임이 종료되었습니다!',
            'UNKNOWN': '🎭 게임이 종료되었습니다!'
        };
        
        return messages[gameResult] || messages['UNKNOWN'];
    }

    // 게임 종료 UI 업데이트
    updateGameEndUI() {
        // 상태 업데이트
        this.updateStatus('currentStatus', '🔴 게임 종료');
        this.updateStatus('roomInfo', '🎯 게임 완료');
        
        // 타이머 영역에 게임 종료 표시
        const timerElement = document.getElementById('timerDisplay');
        if (timerElement) {
            timerElement.innerHTML = '⏹️ 게임 종료';
            timerElement.style.cssText = `
                color: #ff4757;
                font-weight: bold;
                font-size: 18px;
                background: linear-gradient(135deg, #ff6b6b, #ff4757);
                background-clip: text;
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
            `;
        }
        
        // 매칭 버튼들 숨기기
        const startMatchingBtn = document.getElementById('startMatchingBtn');
        const cancelMatchingBtn = document.getElementById('cancelMatchingBtn');
        if (startMatchingBtn) startMatchingBtn.style.display = 'none';
        if (cancelMatchingBtn) cancelMatchingBtn.style.display = 'none';
        
        // 게임 종료 상태 메시지 표시
        const matchingStatus = document.getElementById('matchingStatus');
        if (matchingStatus) {
            matchingStatus.innerHTML = '🎭 게임이 완료되었습니다';
            matchingStatus.style.cssText = `
                color: #ff4757;
                font-weight: bold;
                font-size: 16px;
                text-align: center;
                padding: 10px;
                background: rgba(255, 107, 107, 0.1);
                border-radius: 8px;
                border: 2px solid #ff6b6b;
            `;
            matchingStatus.classList.remove('hidden');
        }
        
        // 페이지 제목 변경
        document.title = '🎭 게임 종료 - 던전톡';
    }

    // 게임 종료 배너 표시
    showGameEndBanner(gameResult = 'UNKNOWN') {
        // 기존 배너가 있으면 제거
        const existingBanner = document.getElementById('gameEndBanner');
        if (existingBanner) {
            existingBanner.remove();
        }
        
        // 결과에 따른 배너 설정
        const bannerConfigs = {
            'SUCCESS': {
                gradient: 'linear-gradient(135deg, #28a745, #20c997)',
                text: '🎉 게임 클리어! 축하합니다! 🎉',
                icon: '🏆'
            },
            'FAILURE': {
                gradient: 'linear-gradient(135deg, #dc3545, #fd7e14)',
                text: '💀 게임 오버! 다음에 다시 도전하세요! 💀',
                icon: '⚰️'
            },
            'TIMEOUT': {
                gradient: 'linear-gradient(135deg, #ffc107, #fd7e14)',
                text: '⏰ 시간 초과! 게임이 종료되었습니다! ⏰',
                icon: '⏱️'
            },
            'UNKNOWN': {
                gradient: 'linear-gradient(135deg, #ff6b6b, #feca57)',
                text: '🎭 게임이 종료되었습니다! 🎭',
                icon: '🎮'
            }
        };
        
        const config = bannerConfigs[gameResult] || bannerConfigs['UNKNOWN'];
        
        // 게임 종료 배너 생성
        const banner = document.createElement('div');
        banner.id = 'gameEndBanner';
        banner.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            background: ${config.gradient};
            color: white;
            text-align: center;
            padding: 15px;
            font-size: 24px;
            font-weight: bold;
            z-index: 9999;
            box-shadow: 0 4px 8px rgba(0,0,0,0.3);
            animation: gameEndSlide 1s ease-out;
        `;
        banner.innerHTML = `
            ${config.icon} <span style="text-shadow: 2px 2px 4px rgba(0,0,0,0.5);">${config.text}</span> ${config.icon}
        `;
        
        // CSS 애니메이션 추가
        if (!document.getElementById('gameEndStyles')) {
            const styles = document.createElement('style');
            styles.id = 'gameEndStyles';
            styles.innerHTML = `
                @keyframes gameEndSlide {
                    from { transform: translateY(-100%); opacity: 0; }
                    to { transform: translateY(0); opacity: 1; }
                }
                @keyframes gameEndPulse {
                    0%, 100% { transform: scale(1); }
                    50% { transform: scale(1.05); }
                }
                .game-ended-overlay {
                    background: rgba(0, 0, 0, 0.7) !important;
                    pointer-events: none;
                }
            `;
            document.head.appendChild(styles);
        }
        
        // 배너를 페이지 상단에 추가
        document.body.insertBefore(banner, document.body.firstChild);
        
        // 전체 페이지에 게임 종료 오버레이 효과
        document.body.classList.add('game-ended-overlay');
        
        // 5초 후 배너 자동 제거
        setTimeout(() => {
            banner.style.animation = 'gameEndSlide 1s ease-in reverse';
            setTimeout(() => {
                if (banner.parentNode) {
                    banner.remove();
                }
            }, 1000);
        }, 5000);
    }

    // 게임 종료 효과 표시
    showGameEndEffect(finalMessage) {
        // 배경 효과
        document.body.style.filter = 'brightness(0.8)';
        
        // 종료 메시지에서 결과 파싱
        let resultType = 'UNKNOWN';
        if (finalMessage.includes('성공') || finalMessage.includes('승리') || finalMessage.includes('완료')) {
            resultType = 'SUCCESS';
        } else if (finalMessage.includes('실패') || finalMessage.includes('패배') || finalMessage.includes('게임오버')) {
            resultType = 'FAILURE';
        } else if (finalMessage.includes('시간') || finalMessage.includes('초과')) {
            resultType = 'TIMEOUT';
        }
        
        // 결과에 따른 메시지 추가
        setTimeout(() => {
            switch (resultType) {
                case 'SUCCESS':
                    this.addAiMessage('system', '🎉 축하합니다! 미션을 성공적으로 완료했습니다!');
                    break;
                case 'FAILURE':
                    this.addAiMessage('system', '💀 아쉽게도 이번 모험은 여기서 끝납니다...');
                    break;
                case 'TIMEOUT':
                    this.addAiMessage('system', '⏰ 시간이 부족했지만 좋은 모험이었습니다!');
                    break;
                default:
                    this.addAiMessage('system', '🎭 모험이 마무리되었습니다. 수고하셨습니다!');
            }
            
            // 게임 재시작 버튼 표시 (선택사항)
            this.addAiMessage('system', '새로운 모험을 시작하려면 페이지를 새로고침하세요.');
        }, 2000);
    }

    // 상태 업데이트 유틸리티
    updateStatus(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
        }
    }

    // 게임 종료 핸들러 등록
    onGameEnd(handler) {
        this.gameEndHandlers.push(handler);
    }

    // 게임 종료 핸들러 실행
    notifyGameEndHandlers(gameResult, finalMessage) {
        this.gameEndHandlers.forEach(handler => {
            try {
                handler(gameResult, finalMessage);
            } catch (error) {
                console.error('게임 종료 핸들러 오류:', error);
            }
        });
    }

    // 채팅 메시지 초기화
    clearMessages() {
        const aiMessages = document.getElementById('aiMessages');
        const rightUserMessages = document.getElementById('rightUserMessages');
        
        if (aiMessages) {
            aiMessages.innerHTML = '';
        }
        if (rightUserMessages) {
            rightUserMessages.innerHTML = '';
        }
    }
}