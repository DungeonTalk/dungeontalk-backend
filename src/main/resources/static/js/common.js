// 글로벌 알림 시스템
const GlobalNotification = {
    notifications: [],
    nextId: 1,
    
    // 알림 추가
    add(type, content) {
        const notification = {
            id: this.nextId++,
            type: type,
            content: content,
            timestamp: Date.now()
        };
        
        this.notifications.push(notification);
        this.render(notification);
        
        // 2초 후 자동 제거
        setTimeout(() => {
            this.remove(notification.id);
        }, 2000);
        
        return notification.id;
    },
    
    // 알림 제거
    remove(id) {
        const notificationElement = document.getElementById(`notification-${id}`);
        if (notificationElement) {
            notificationElement.classList.add('hide');
            
            setTimeout(() => {
                notificationElement.remove();
                this.notifications = this.notifications.filter(n => n.id !== id);
            }, 500);
        }
    },
    
    // 알림 렌더링
    render(notification) {
        const container = document.getElementById('globalNotifications');
        if (!container) {
            console.warn('GlobalNotifications container not found');
            return;
        }
        
        const notificationElement = document.createElement('div');
        notificationElement.id = `notification-${notification.id}`;
        notificationElement.className = 'notification';
        
        notificationElement.innerHTML = `
            <div class="notification-content">
                <div class="notification-main">
                    <div class="notification-icon ${notification.type}">
                        ${this.getIcon(notification.type)}
                    </div>
                    <div class="notification-text">
                        <div class="notification-type">${notification.type}</div>
                        <div class="notification-message">${notification.content}</div>
                    </div>
                </div>
                <div class="notification-close" onclick="GlobalNotification.remove(${notification.id})">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                    </svg>
                </div>
            </div>
        `;
        
        container.appendChild(notificationElement);
        
        // 애니메이션을 위한 약간의 지연
        setTimeout(() => {
            notificationElement.classList.add('show');
        }, 10);
    },
    
    // 타입별 아이콘 반환
    getIcon(type) {
        switch(type) {
            case 'info':
                return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                    <path fill-rule="evenodd" d="M15 8A7 7 0 1 1 1 8a7 7 0 0 1 14 0ZM9 5a1 1 0 1 1-2 0 1 1 0 0 1 2 0ZM6.75 8a.75.75 0 0 0 0 1.5h.75v1.75a.75.75 0 0 0 1.5 0v-2.5A.75.75 0 0 0 8.25 8h-1.5Z" clip-rule="evenodd" />
                </svg>`;
            case 'success':
                return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                    <path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14Zm3.844-8.791a.75.75 0 0 0-1.188-.918l-3.7 4.79-1.649-1.833a.75.75 0 1 0-1.114 1.004l2.25 2.5a.75.75 0 0 0 1.15-.043l4.25-5.5Z" clip-rule="evenodd"></path>
                </svg>`;
            case 'error':
                return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor">
                    <path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14ZM8 4a.75.75 0 0 1 .75.75v3a.75.75 0 0 1-1.5 0v-3A.75.75 0 0 1 8 4Zm0 8a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" clip-rule="evenodd"></path>
                </svg>`;
            default:
                return '';
        }
    },
    
    // 편의 함수들
    info(content) {
        return this.add('info', content);
    },
    
    success(content) {
        return this.add('success', content);
    },
    
    error(content) {
        return this.add('error', content);
    }
};

// 공통 인증 관리
const AuthManager = {
    // 토큰 저장
    setAuthData(token, user) {
        localStorage.setItem('authToken', token);
        localStorage.setItem('currentUser', JSON.stringify(user));
    },
    
    // 토큰 가져오기
    getAuthToken() {
        return localStorage.getItem('authToken');
    },
    
    // 사용자 정보 가져오기
    getCurrentUser() {
        const userInfo = localStorage.getItem('currentUser');
        if (userInfo) {
            try {
                return JSON.parse(userInfo);
            } catch (e) {
                console.error('사용자 정보 파싱 오류:', e);
                return null;
            }
        }
        return null;
    },
    
    // 인증 상태 확인
    isAuthenticated() {
        const token = this.getAuthToken();
        const user = this.getCurrentUser();
        return token && token !== 'null' && token.trim() !== '' && user;
    },
    
    // 인증 데이터 삭제
    clearAuthData() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('currentUser');
    }
};

// 공통 API 호출 함수
const ApiClient = {
    // 기본 헤더 생성
    getHeaders(includeAuth = true) {
        const headers = {
            'Content-Type': 'application/json',
            'Cache-Control': 'no-cache'
        };
        
        if (includeAuth) {
            const token = AuthManager.getAuthToken();
            if (token) {
                headers['Authorization'] = 'Bearer ' + token;
            }
        }
        
        return headers;
    },
    
    // GET 요청
    async get(url, includeAuth = true) {
        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: this.getHeaders(includeAuth)
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('GET 요청 오류:', error);
            throw error;
        }
    },
    
    // POST 요청
    async post(url, data, includeAuth = true) {
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: this.getHeaders(includeAuth),
                body: JSON.stringify(data)
            });
            return await this.handleResponse(response);
        } catch (error) {
            console.error('POST 요청 오류:', error);
            throw error;
        }
    },
    
    // DELETE 요청
    async delete(url, data = null, includeAuth = true) {
        try {
            const options = {
                method: 'DELETE',
                headers: this.getHeaders(includeAuth)
            };
            
            if (data) {
                options.body = JSON.stringify(data);
            }
            
            const response = await fetch(url, options);
            return await this.handleResponse(response);
        } catch (error) {
            console.error('DELETE 요청 오류:', error);
            throw error;
        }
    },
    
    // 응답 처리
    async handleResponse(response) {
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            const result = await response.json();
            return { response, result };
        } else {
            const text = await response.text();
            return { response, result: { text } };
        }
    }
};

// WebSocket 관리 클래스
class WebSocketManager {
    constructor() {
        this.connections = new Map();
    }
    
    // 연결 생성
    connect(name, url, onConnect, onMessage, onError) {
        try {
            // 기존 연결 종료
            this.disconnect(name);
            
            console.log(`${name} WebSocket 연결 시도:`, url);
            
            const socket = new SockJS(url);
            const stompClient = Stomp.over(socket);
            
            stompClient.connect({}, (frame) => {
                console.log(`✅ ${name} WebSocket 연결 성공:`, frame);
                this.connections.set(name, stompClient);
                if (onConnect) onConnect(stompClient, frame);
            }, (error) => {
                console.error(`❌ ${name} WebSocket 연결 실패:`, error);
                if (onError) onError(error);
            });
            
        } catch (error) {
            console.error(`${name} WebSocket 오류:`, error);
            if (onError) onError(error);
        }
    }
    
    // 연결 종료
    disconnect(name) {
        const connection = this.connections.get(name);
        if (connection && connection.connected) {
            try {
                connection.disconnect();
                console.log(`✅ ${name} WebSocket 연결 해제 완료`);
            } catch (error) {
                console.error(`❌ ${name} WebSocket 해제 오류:`, error);
            }
        }
        this.connections.delete(name);
    }
    
    // 연결 가져오기
    getConnection(name) {
        return this.connections.get(name);
    }
    
    // 메시지 전송
    send(name, destination, data) {
        const connection = this.connections.get(name);
        if (connection && connection.connected) {
            connection.send(destination, {}, JSON.stringify(data));
            return true;
        } else {
            console.error(`${name} WebSocket이 연결되지 않음`);
            return false;
        }
    }
    
    // 구독
    subscribe(name, destination, callback) {
        const connection = this.connections.get(name);
        if (connection && connection.connected) {
            return connection.subscribe(destination, callback);
        } else {
            console.error(`${name} WebSocket이 연결되지 않음`);
            return null;
        }
    }
    
    // 모든 연결 종료
    disconnectAll() {
        for (const [name, connection] of this.connections) {
            this.disconnect(name);
        }
    }
}

// 공통 유틸리티 함수들
const Utils = {
    // HTML 이스케이프
    escapeHtml(content) {
        return content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;');
    },
    
    // 마크다운 스타일 포맷팅
    formatMessage(content) {
        const escapedContent = this.escapeHtml(content);
        
        return escapedContent
            .replace(/\n/g, '<br>')           // \n을 <br>로 변환
            .replace(/\r\n/g, '<br>')        // \r\n을 <br>로 변환  
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **텍스트**를 굵게
            .replace(/\*(.*?)\*/g, '<em>$1</em>')            // *텍스트*를 기울임
            .replace(/`(.*?)`/g, '<code>$1</code>');         // `코드`를 코드 스타일
    },
    
    // 상태 업데이트
    updateStatus(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
        }
    },
    
    // 요소 표시/숨김
    toggleElement(elementId, show) {
        const element = document.getElementById(elementId);
        if (element) {
            if (show) {
                element.classList.remove('hidden');
            } else {
                element.classList.add('hidden');
            }
        }
    },
    
    // 폼 데이터 가져오기
    getFormData(formId) {
        const form = document.getElementById(formId);
        if (!form) return {};
        
        const formData = new FormData(form);
        const data = {};
        
        for (let [key, value] of formData.entries()) {
            data[key] = value.trim();
        }
        
        return data;
    },
    
    // 입력 필드 검증
    validateRequired(fields) {
        for (const field of fields) {
            if (!field.value || field.value.trim() === '') {
                return false;
            }
        }
        return true;
    }
};

// 전역 WebSocket 매니저 인스턴스
const wsManager = new WebSocketManager();

// 페이지 언로드 시 모든 WebSocket 연결 해제
window.addEventListener('beforeunload', () => {
    wsManager.disconnectAll();
});

// 전역 변수로 노출 (하위 호환성)
window.GlobalNotification = GlobalNotification;
window.AuthManager = AuthManager;
window.ApiClient = ApiClient;
window.WebSocketManager = WebSocketManager;
window.wsManager = wsManager;
window.Utils = Utils;