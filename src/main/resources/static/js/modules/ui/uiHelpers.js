// UI 유틸리티 및 헬퍼 함수들
export class UIHelpers {
    constructor() {
        this.setupWindowEventListeners();
    }

    // 상태 업데이트 유틸리티
    updateStatus(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
        } else {
            console.warn(`${elementId} 엘리먼트를 찾을 수 없습니다.`);
        }
    }

    // 로그인 상태 UI 업데이트
    showLoggedInState(currentUser) {
        const loginSection = document.getElementById('loginSection');
        const matchingSection = document.getElementById('matchingSection');
        const headerActions = document.getElementById('headerActions');
        
        if (loginSection) loginSection.classList.add('hidden');
        if (matchingSection) matchingSection.classList.remove('hidden');
        if (headerActions) headerActions.style.display = 'flex';
        
        this.updateStatus('currentStatus', '매칭 대기');
        this.updateStatus('currentUser', currentUser.name);
    }

    // 매칭 화면으로 복귀
    showMatchingScreen(selectedWorld = null) {
        const gameSection = document.getElementById('gameSection');
        const matchingSection = document.getElementById('matchingSection');
        const leaveGameCard = document.getElementById('leaveGameCard');
        
        // UI 전환
        if (gameSection) gameSection.classList.add('hidden');
        if (matchingSection) matchingSection.classList.remove('hidden');
        if (leaveGameCard) leaveGameCard.style.display = 'none';
        
        // 상태 업데이트
        this.updateStatus('currentStatus', '매칭 대기');
        this.updateStatus('matchingInfo', '대기 중');
        this.updateStatus('roomInfo', '미연결');
        
        // 매칭 버튼 상태 초기화
        const startMatchingBtn = document.getElementById('startMatchingBtn');
        const cancelMatchingBtn = document.getElementById('cancelMatchingBtn');
        const matchingStatus = document.getElementById('matchingStatus');
        
        if (startMatchingBtn) startMatchingBtn.classList.remove('hidden');
        if (cancelMatchingBtn) cancelMatchingBtn.classList.add('hidden');
        if (matchingStatus) matchingStatus.classList.add('hidden');
        if (startMatchingBtn) startMatchingBtn.disabled = selectedWorld ? false : true;
    }

    // 게임 화면으로 전환
    showGameScreen(matchData) {
        const matchingSection = document.getElementById('matchingSection');
        const gameSection = document.getElementById('gameSection');
        const leaveGameCard = document.getElementById('leaveGameCard');
        
        // UI 전환
        if (matchingSection) matchingSection.classList.add('hidden');
        if (gameSection) gameSection.classList.remove('hidden');
        
        // 상태 업데이트
        this.updateStatus('currentStatus', '게임 중');
        this.updateStatus('matchingInfo', '매칭 완료');
        
        if (matchData.aiGameRoomId && matchData.chatRoomId) {
            this.updateStatus('roomInfo', `AI: ${matchData.aiGameRoomId.substring(0, 8)}... | 파티: ${matchData.chatRoomId.substring(0, 8)}...`);
        }
        
        // 퇴장 버튼 표시
        if (leaveGameCard) leaveGameCard.style.display = 'block';
    }

    // 매칭 상태 UI 업데이트
    updateMatchingUI(isMatching, worldType) {
        const startMatchingBtn = document.getElementById('startMatchingBtn');
        const cancelMatchingBtn = document.getElementById('cancelMatchingBtn');
        const matchingStatus = document.getElementById('matchingStatus');
        
        if (isMatching) {
            if (startMatchingBtn) startMatchingBtn.classList.add('hidden');
            if (cancelMatchingBtn) cancelMatchingBtn.classList.remove('hidden');
            if (matchingStatus) matchingStatus.classList.remove('hidden');
            
            this.updateStatus('matchingInfo', `${this.getWorldName(worldType)} 매칭 중`);
        } else {
            if (startMatchingBtn) startMatchingBtn.classList.remove('hidden');
            if (cancelMatchingBtn) cancelMatchingBtn.classList.add('hidden');
            if (matchingStatus) matchingStatus.classList.add('hidden');
            
            this.updateStatus('matchingInfo', '대기 중');
        }
    }

    // 큐 상태 업데이트
    updateQueueStatus(data) {
        const queueInfo = document.getElementById('queueInfo');
        if (queueInfo) {
            queueInfo.innerHTML = `
                대기 순서: ${data.currentPosition}번째 | 
                총 대기자: ${data.totalInQueue}명 | 
                예상 시간: ${data.estimatedMessage}
            `;
        }
    }

    // 채팅 입력 활성화
    enableChatInputs() {
        const elements = [
            'aiMessageInput', 'aiSendBtn', 'aiRequestBtn',
            'rightUserMessageInput', 'rightUserSendBtn'
        ];
        
        elements.forEach(id => {
            const element = document.getElementById(id);
            if (element) element.disabled = false;
        });
    }

    // 채팅 입력 비활성화
    disableChatInputs() {
        const elements = [
            'aiMessageInput', 'aiSendBtn', 'aiRequestBtn',
            'rightUserMessageInput', 'rightUserSendBtn'
        ];
        
        elements.forEach(id => {
            const element = document.getElementById(id);
            if (element) element.disabled = true;
        });
    }

    // 엔터키 이벤트 처리
    setupEnterKeyHandlers() {
        // AI 메시지 입력 엔터키 처리
        const aiMessageInput = document.getElementById('aiMessageInput');
        if (aiMessageInput) {
            aiMessageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    if (window.sendAiMessage) {
                        window.sendAiMessage();
                    }
                }
            });
        }
        
        // 파티 메시지 입력 엔터키 처리
        const rightUserMessageInput = document.getElementById('rightUserMessageInput');
        if (rightUserMessageInput) {
            rightUserMessageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    if (window.sendRightUserMessage) {
                        window.sendRightUserMessage();
                    }
                }
            });
        }
    }

    // 로그인 폼 엔터키 처리
    setupLoginFormHandlers() {
        const userIdInput = document.getElementById('userId');
        const passwordInput = document.getElementById('password');
        
        [userIdInput, passwordInput].forEach(input => {
            if (input) {
                input.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        if (window.login) {
                            window.login();
                        }
                    }
                });
            }
        });
    }

    // 윈도우 이벤트 리스너 설정
    setupWindowEventListeners() {
        // 페이지 새로고침 시 처리
        window.addEventListener('load', () => {
            console.log('🔄 페이지 로드 완료');
        });
        
        // 페이지 언로드 시 처리
        window.addEventListener('beforeunload', (e) => {
            // WebSocket 연결들 정리는 각 모듈에서 처리
            console.log('🔄 페이지 언로드');
        });
    }

    // 로딩 스피너 표시
    showLoading(message = '로딩 중...') {
        let loadingElement = document.getElementById('loadingSpinner');
        
        if (!loadingElement) {
            loadingElement = document.createElement('div');
            loadingElement.id = 'loadingSpinner';
            loadingElement.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 10000;
                color: white;
                font-size: 18px;
            `;
            document.body.appendChild(loadingElement);
        }
        
        loadingElement.innerHTML = `
            <div style="text-align: center;">
                <div style="border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; width: 50px; height: 50px; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
                <div>${message}</div>
            </div>
        `;
        
        // CSS 애니메이션 추가
        if (!document.getElementById('loadingSpinnerStyles')) {
            const styles = document.createElement('style');
            styles.id = 'loadingSpinnerStyles';
            styles.innerHTML = `
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            `;
            document.head.appendChild(styles);
        }
        
        loadingElement.style.display = 'flex';
    }

    // 로딩 스피너 숨기기
    hideLoading() {
        const loadingElement = document.getElementById('loadingSpinner');
        if (loadingElement) {
            loadingElement.style.display = 'none';
        }
    }

    // 알림 메시지 표시
    showNotification(message, type = 'info', duration = 3000) {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'error' ? '#f44336' : type === 'success' ? '#4caf50' : '#2196f3'};
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10001;
            font-size: 14px;
            box-shadow: 0 4px 8px rgba(0,0,0,0.2);
            animation: slideInFromRight 0.3s ease-out;
        `;
        notification.textContent = message;
        
        // CSS 애니메이션 추가
        if (!document.getElementById('notificationStyles')) {
            const styles = document.createElement('style');
            styles.id = 'notificationStyles';
            styles.innerHTML = `
                @keyframes slideInFromRight {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideOutToRight {
                    from { transform: translateX(0); opacity: 1; }
                    to { transform: translateX(100%); opacity: 0; }
                }
            `;
            document.head.appendChild(styles);
        }
        
        document.body.appendChild(notification);
        
        // 자동 제거
        setTimeout(() => {
            notification.style.animation = 'slideOutToRight 0.3s ease-in';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, duration);
    }

    // 세계관 이름 가져오기 (중복 메서드이지만 독립성을 위해 포함)
    getWorldName(worldType) {
        const names = {
            'FANTASY': '🏰 판타지',
            'ZOMBIE': '🧟 좀비'
        };
        return names[worldType] || worldType;
    }

    // 디버그 정보 표시 (개발용)
    showDebugInfo(info) {
        if (process?.env?.NODE_ENV === 'development' || window.location.hostname === 'localhost') {
            console.log('🐛 Debug Info:', info);
        }
    }
}