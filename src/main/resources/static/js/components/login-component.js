export default {
    template: `
        <div class="max-w-md mx-auto mb-10">
            <div class="glass-card p-8">
                <h2 class="text-2xl font-bold mb-6 text-center">모험가 등록</h2>
                
                <div class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium mb-2 text-light-darker">사용자 ID</label>
                        <input 
                            v-model="loginForm.userId"
                            type="text" 
                            class="modern-input w-full"
                            placeholder="사용자 ID를 입력하세요"
                            @keyup.enter="login"
                        >
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium mb-2 text-light-darker">비밀번호</label>
                        <input 
                            v-model="loginForm.password"
                            type="password" 
                            class="modern-input w-full"
                            placeholder="비밀번호를 입력하세요"
                            @keyup.enter="login"
                        >
                    </div>
                    
                    <div class="grid grid-cols-2 gap-4 mt-6">
                        <button @click="register" class="neo-btn w-full">
                            회원가입
                        </button>
                        <button @click="login" class="neo-btn w-full bg-gradient-to-r from-success to-green-600">
                            로그인
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            loginForm: {
                userId: 'player1',
                password: 'password123'
            }
        };
    },
    methods: {
        async register() {
            if (!this.loginForm.userId || !this.loginForm.password) {
                alert('ID와 비밀번호를 입력하세요.');
                return;
            }
            
            try {
                const response = await fetch('/v1/member/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: this.loginForm.userId,
                        nickName: this.loginForm.userId + '님',
                        password: this.loginForm.password
                    })
                });
                
                if (response.ok) {
                    alert('회원가입 성공! 이제 로그인하세요.');
                } else {
                    const error = await response.text();
                    alert('회원가입 실패: ' + error);
                }
            } catch (error) {
                alert('회원가입 오류: ' + error.message);
            }
        },
        
        async login() {
            if (!this.loginForm.userId || !this.loginForm.password) {
                alert('ID와 비밀번호를 입력하세요.');
                return;
            }
            
            try {
                const response = await fetch('/v1/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: this.loginForm.userId,
                        password: this.loginForm.password
                    })
                });
                
                if (response.ok) {
                    const responseData = await response.json();
                    const loginData = responseData.data;
                    
                    this.$emit('login-success', {
                        authToken: loginData.accessToken,
                        userInfo: this.extractUserInfoFromToken(loginData.accessToken)
                    });
                } else {
                    const error = await response.text();
                    alert('로그인 실패: ' + error);
                }
            } catch (error) {
                alert('로그인 오류: ' + error.message);
            }
        },
        
        extractUserInfoFromToken(token) {
            try {
                const base64Url = token.split('.')[1];
                const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
                
                const payload = JSON.parse(jsonPayload);
                return {
                    id: payload.id,
                    name: payload.name || payload.sub || 'Unknown'
                };
            } catch (error) {
                console.error('JWT 파싱 오류:', error);
                return null;
            }
        }
    }
};