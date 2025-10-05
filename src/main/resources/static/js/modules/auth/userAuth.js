// 사용자 인증 관련 기능
export class UserAuth {
    constructor() {
        this.authToken = null;
        this.currentUser = null;
    }

    // 기존 로그인 확인
    checkExistingLogin() {
        this.authToken = localStorage.getItem('authToken');
        const userInfo = localStorage.getItem('currentUser');
        
        console.log('인증 체크:', { 
            authToken: this.authToken ? ('존재: ' + this.authToken.substring(0, 10) + '...') : 'null', 
            userInfo: userInfo ? '존재' : 'null' 
        });
        
        if (this.authToken && this.authToken !== 'null' && this.authToken.trim() !== '' && userInfo) {
            try {
                this.currentUser = JSON.parse(userInfo);
                console.log('✅ 로그인 상태 확인:', this.currentUser.name);
                return true;
            } catch (e) {
                console.error('❌ 사용자 정보 파싱 오류:', e);
                this.clearAuthData();
                return false;
            }
        } else {
            console.log('⚠️ 로그인 필요');
            this.clearAuthData();
            return false;
        }
    }

    clearAuthData() {
        this.authToken = null;
        this.currentUser = null;
        localStorage.clear();
        console.log('🗑️ 인증 데이터 삭제 완료');
    }

    // 회원가입
    async register(userId, password) {
        if (!userId || !password) {
            throw new Error('ID와 비밀번호를 입력하세요.');
        }
        
        console.log('회원가입 시도:', { name: userId, nickName: userId + '님' });
        
        try {
            const response = await fetch('/v1/member/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: userId,
                    nickName: userId + '님',
                    password: password
                })
            });
            
            console.log('회원가입 응답 상태:', response.status);
            
            if (response.ok) {
                const result = await response.json();
                console.log('회원가입 성공:', result);
                return { success: true, message: '회원가입 성공! 이제 로그인하세요.' };
            } else {
                const errorText = await response.text();
                console.error('회원가입 실패:', errorText);
                try {
                    const errorJson = JSON.parse(errorText);
                    return { success: false, message: errorJson.msg || errorJson.message || errorText };
                } catch (e) {
                    return { success: false, message: errorText };
                }
            }
        } catch (error) {
            return { success: false, message: error.message };
        }
    }

    // 로그인
    async login(userId, password) {
        if (!userId || !password) {
            throw new Error('ID와 비밀번호를 입력하세요.');
        }
        
        try {
            const response = await fetch('/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: userId,
                    password: password
                })
            });
            
            if (response.ok) {
                const responseData = await response.json();
                const loginData = responseData.data;
                
                console.log('로그인 응답 데이터:', loginData);
                
                // JWT 토큰 저장
                this.authToken = loginData.accessToken;
                
                // JWT 토큰에서 사용자 정보 추출
                this.currentUser = this.extractUserInfoFromToken(this.authToken);
                
                if (!this.currentUser) {
                    throw new Error('JWT 토큰에서 사용자 정보를 추출할 수 없습니다.');
                }
                
                console.log('JWT에서 추출한 사용자 정보:', this.currentUser);
                
                localStorage.setItem('authToken', this.authToken);
                localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
                
                return { success: true, user: this.currentUser };
            } else {
                const error = await response.text();
                return { success: false, message: error };
            }
        } catch (error) {
            return { success: false, message: error.message };
        }
    }

    // JWT 토큰 디코딩
    decodeJWT(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error('JWT 디코딩 오류:', error);
            return null;
        }
    }

    // JWT에서 사용자 정보 추출
    extractUserInfoFromToken(token) {
        const payload = this.decodeJWT(token);
        if (payload) {
            return {
                id: payload.id || payload.sub,
                name: payload.name,
                nickname: payload.nickName
            };
        }
        return null;
    }

    // 로그아웃
    logout() {
        this.clearAuthData();
        return true;
    }

    // getter methods
    getAuthToken() {
        return this.authToken;
    }

    getCurrentUser() {
        return this.currentUser;
    }

    isAuthenticated() {
        return this.authToken && this.currentUser;
    }
}