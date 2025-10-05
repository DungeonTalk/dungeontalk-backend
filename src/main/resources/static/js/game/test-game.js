// Static HTML용 게임 테스트 JS (Authorization 헤더 사용)
// 이 파일은 test-auth.html 등 static HTML에서 사용

async function testCharacterAPI() {
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        console.error('토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }
    
    // V1 API 테스트 (Authorization 헤더 사용)
    try {
        const response = await fetch('/v1/characters/my', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            console.log('캐릭터 조회 성공:', data);
            return data;
        } else {
            console.error('캐릭터 조회 실패:', response.status);
        }
    } catch (error) {
        console.error('API 호출 오류:', error);
    }
}

async function createCharacterV1(memberId, race) {
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        console.error('토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }
    
    try {
        const response = await fetch('/v1/characters', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                memberId: memberId,
                race: race
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            console.log('캐릭터 생성 성공:', data);
            return data;
        } else {
            console.error('캐릭터 생성 실패:', response.status);
        }
    } catch (error) {
        console.error('API 호출 오류:', error);
    }
}

// WebSocket 연결 테스트 (Authorization 헤더 사용)
function connectWebSocketWithToken() {
    const token = localStorage.getItem('authToken');
    
    if (!token) {
        console.error('토큰이 없습니다.');
        return;
    }
    
    const socket = new SockJS('/ws-chat');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect(
        { Authorization: 'Bearer ' + token },
        () => {
            console.log('WebSocket 연결 성공');
            
            // 구독 예제
            stompClient.subscribe('/topic/messages', (message) => {
                console.log('메시지 수신:', message.body);
            });
        },
        (error) => {
            console.error('WebSocket 연결 실패:', error);
        }
    );
    
    return stompClient;
}

// 로그인 테스트 함수
async function testLogin(nickname, password) {
    try {
        const response = await fetch('/v1/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: nickname,
                password: password
            })
        });
        
        if (response.ok) {
            const data = await response.json();
            console.log('로그인 성공:', data);
            
            // 토큰 저장
            if (data.data && data.data.accessToken) {
                localStorage.setItem('authToken', data.data.accessToken);
                console.log('토큰 저장 완료');
            }
            
            return data;
        } else {
            console.error('로그인 실패:', response.status);
        }
    } catch (error) {
        console.error('로그인 오류:', error);
    }
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
    console.log('Static HTML Game Test JS 로드됨');
    console.log('사용 가능한 함수:');
    console.log('- testLogin(nickname, password)');
    console.log('- testCharacterAPI()');
    console.log('- createCharacterV1(memberId, race)');
    console.log('- connectWebSocketWithToken()');
});