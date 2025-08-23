const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ 
        headless: false,  // 브라우저 UI 표시
        devtools: true    // 개발자 도구 열기
    });
    const context = await browser.newContext();
    const page = await context.newPage();
    
    // 네트워크 요청 모니터링
    page.on('requestfailed', request => {
        console.log('❌ Request failed:', request.url(), request.failure().errorText);
    });
    
    page.on('response', response => {
        if (response.status() >= 400) {
            console.log(`❌ HTTP ${response.status()}: ${response.url()}`);
            response.text().then(body => {
                console.log('Response body:', body);
            }).catch(() => {});
        }
    });
    
    // 콘솔 로그 캡처
    page.on('console', msg => {
        if (msg.type() === 'error') {
            console.log('🔴 Console error:', msg.text());
        } else if (msg.type() === 'log' && msg.text().includes('캐릭터')) {
            console.log('📝 Console log:', msg.text());
        }
    });
    
    try {
        console.log('1. 로그인 페이지로 이동...');
        await page.goto('http://localhost:8080/login');
        await page.waitForLoadState('networkidle');
        
        // 새로운 계정으로 회원가입 시도 (또는 기존 계정 사용)
        const testUsername = `testuser_${Date.now()}`;
        const testPassword = 'password123';
        
        console.log(`2. 로그인 시도 (사용자: ${testUsername})...`);
        
        // 회원가입 API 호출 (V2 API 사용)
        const signupResponse = await page.evaluate(async ({ username, password }) => {
            try {
                const response = await fetch('/v2/members', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        email: username,
                        password: password,
                        nickname: username.split('@')[0] || username
                    })
                });
                return {
                    status: response.status,
                    ok: response.ok,
                    data: await response.text()
                };
            } catch (error) {
                return { error: error.message };
            }
        }, { username: testUsername, password: testPassword });
        
        console.log('회원가입 응답:', signupResponse);
        
        // 로그인 - 더 구체적인 selector 사용
        await page.locator('input[type="text"]').first().fill(testUsername);
        await page.fill('input[name="password"], input[type="password"]', testPassword);
        await page.click('button:has-text("로그인")');
        
        // 게임 페이지로 이동 대기
        await page.waitForURL('**/game', { timeout: 10000 });
        console.log('3. 게임 페이지 도착');
        
        // 캐릭터 버튼 클릭
        console.log('4. 내 캐릭터 버튼 클릭...');
        await page.click('button:has-text("내 캐릭터")');
        await page.waitForTimeout(1000);
        
        // 캐릭터가 없는지 확인
        const noCharacterText = await page.locator('text=캐릭터가 없습니다').isVisible();
        console.log('캐릭터 없음 확인:', noCharacterText);
        
        if (noCharacterText) {
            console.log('5. 캐릭터 생성 시도...');
            
            // 종족 선택
            const races = ['HUMAN', 'ELF', 'DWARF', 'ORC'];
            const selectedRace = races[0]; // HUMAN 선택
            
            // 종족 선택 - Alpine.js x-on:click 처리
            await page.evaluate((race) => {
                const element = document.querySelector('[x-data*="gameApp"]') || document.querySelector('[x-data]');
                if (element && element.__x) {
                    element.__x.$data.selectedRace = race;
                    element.__x.$data.updateRaceDescription();
                }
            }, selectedRace);
            console.log(`종족 선택: ${selectedRace}`);
            
            // 네트워크 요청 캡처 준비
            const [response] = await Promise.all([
                page.waitForResponse(resp => resp.url().includes('/v2/characters') && resp.request().method() === 'POST'),
                page.click('button:has-text("캐릭터 생성")')
            ]);
            
            console.log('==========================================');
            console.log('캐릭터 생성 요청 정보:');
            console.log('URL:', response.url());
            console.log('Status:', response.status());
            console.log('Headers:', response.headers());
            
            const requestHeaders = response.request().headers();
            console.log('Request Headers:', requestHeaders);
            console.log('Content-Type:', requestHeaders['content-type']);
            
            const requestBody = response.request().postData();
            console.log('Request Body:', requestBody);
            
            if (response.status() === 415) {
                console.log('❌ 415 에러 발생!');
                const responseBody = await response.text();
                console.log('Response Body:', responseBody);
                
                // Alpine.js 상태 확인
                const alpineData = await page.evaluate(() => {
                    const element = document.querySelector('[x-data]');
                    if (element && element.__x) {
                        return {
                            selectedRace: element.__x.$data.selectedRace,
                            authToken: element.__x.$data.authToken,
                            currentUser: element.__x.$data.currentUser
                        };
                    }
                    return null;
                });
                console.log('Alpine.js Data:', alpineData);
                
                // 실제 전송된 데이터 확인
                const actualRequest = await page.evaluate(() => {
                    // createCharacter 함수 내용 확인
                    const gameApp = window.gameApp ? window.gameApp() : null;
                    if (gameApp) {
                        return {
                            selectedRace: gameApp.selectedRace,
                            authToken: gameApp.authToken
                        };
                    }
                    return null;
                });
                console.log('Game App Data:', actualRequest);
            } else if (response.ok()) {
                console.log('✅ 캐릭터 생성 성공!');
                const responseBody = await response.json();
                console.log('Response:', responseBody);
            }
            console.log('==========================================');
        }
        
    } catch (error) {
        console.error('테스트 중 오류 발생:', error);
    } finally {
        // 브라우저는 열어둠 (디버깅용)
        console.log('\n테스트 완료. 브라우저를 수동으로 닫으세요.');
        // await browser.close();
    }
})();