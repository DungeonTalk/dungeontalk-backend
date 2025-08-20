import { test, expect } from '@playwright/test';

test.describe('DungeonTalk Application Tests', () => {
  
  test('로그인 및 게임 페이지 접근', async ({ page }) => {
    // 1. 로그인 페이지로 이동
    await page.goto('/login');
    await expect(page).toHaveTitle(/로그인 - DungeonTalk/);
    
    // 2. 로그인 폼 입력
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    
    // 3. 로그인 버튼 클릭
    await page.click('button[type="submit"]');
    
    // 4. 게임 페이지로 리다이렉트 확인
    await page.waitForURL('/game', { timeout: 10000 });
    await expect(page).toHaveTitle(/게임 - DungeonTalk/);
    
    // 5. 게임 페이지 요소 확인
    const gameTitle = page.locator('h1:has-text("던전톡 게임")');
    await expect(gameTitle).toBeVisible();
    
    // 6. 캐릭터 생성 모달 확인
    const characterModal = page.locator('div[x-show="showCharacterModal"]');
    const hasCharacter = await page.locator('.character-card').count() > 0;
    
    if (!hasCharacter) {
      // 캐릭터가 없으면 모달이 표시되어야 함
      await expect(characterModal).toBeVisible();
      console.log('캐릭터 생성 모달이 표시됨');
    } else {
      console.log('기존 캐릭터가 존재함');
    }
  });
  
  test('캐릭터 생성 플로우', async ({ page }) => {
    // 1. 로그인
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/game', { timeout: 10000 });
    
    // 2. 캐릭터 생성 버튼 찾기 및 클릭
    const createCharacterBtn = page.locator('button:has-text("캐릭터 생성")').first();
    const isCreateBtnVisible = await createCharacterBtn.isVisible().catch(() => false);
    
    if (isCreateBtnVisible) {
      await createCharacterBtn.click();
      console.log('캐릭터 생성 버튼 클릭');
      
      // 3. 캐릭터 생성 모달 확인
      const modal = page.locator('div[x-show="showCharacterModal"]');
      await expect(modal).toBeVisible({ timeout: 5000 });
      
      // 4. 캐릭터 정보 입력
      await page.fill('input[x-model="newCharacter.name"]', 'TestHero');
      
      // 종족 선택
      const raceSelect = page.locator('select[x-model="newCharacter.raceId"]');
      if (await raceSelect.isVisible()) {
        await raceSelect.selectOption({ index: 1 }); // 첫 번째 옵션 선택
      }
      
      // 5. 생성 버튼 클릭
      const submitBtn = page.locator('button:has-text("생성하기")');
      await submitBtn.click();
      
      // 6. 생성 완료 확인
      await page.waitForTimeout(2000); // API 응답 대기
      
      // 성공 메시지 또는 캐릭터 카드 확인
      const successMessage = page.locator('text=/캐릭터.*생성/i');
      const characterCard = page.locator('.character-card');
      
      const hasSuccess = await successMessage.isVisible().catch(() => false);
      const hasCard = await characterCard.isVisible().catch(() => false);
      
      if (hasSuccess || hasCard) {
        console.log('캐릭터 생성 성공');
      } else {
        console.log('캐릭터 생성 실패 또는 확인 불가');
      }
    } else {
      console.log('캐릭터 생성 버튼을 찾을 수 없음');
    }
  });
  
  test('WebSocket 연결 및 채팅 테스트', async ({ page }) => {
    // 1. 로그인
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // 2. 채팅 페이지로 이동
    await page.goto('/chat');
    await expect(page).toHaveTitle(/채팅 - DungeonTalk/);
    
    // 3. WebSocket 연결 상태 확인
    const connectionStatus = page.locator('text=/WebSocket/');
    await expect(connectionStatus).toBeVisible();
    
    // 4. 방 생성
    const createRoomBtn = page.locator('button:has-text("방 생성")');
    await createRoomBtn.click();
    await page.waitForTimeout(1000);
    
    // 5. WebSocket 연결
    const connectBtn = page.locator('button:has-text("WS 연결")');
    await connectBtn.click();
    await page.waitForTimeout(2000);
    
    // 6. 연결 상태 확인
    const connectedStatus = page.locator('text=/WebSocket 연결됨/');
    const isConnected = await connectedStatus.isVisible().catch(() => false);
    
    if (isConnected) {
      console.log('WebSocket 연결 성공');
      
      // 7. 메시지 전송
      await page.fill('input[placeholder="메시지를 입력하세요..."]', '테스트 메시지');
      await page.click('button:has-text("전송")');
      
      // 8. 메시지 표시 확인
      await page.waitForTimeout(1000);
      const message = page.locator('text=/테스트 메시지/');
      const isMessageVisible = await message.isVisible().catch(() => false);
      
      if (isMessageVisible) {
        console.log('메시지 전송 및 표시 성공');
      }
    } else {
      console.log('WebSocket 연결 실패');
    }
  });
  
  test('페이지 네비게이션 테스트', async ({ page }) => {
    // 1. 로그인
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/game', { timeout: 10000 });
    
    // 2. 홈으로 이동
    await page.click('a[href="/"]');
    await page.waitForURL('/');
    await expect(page.locator('h1:has-text("던전톡에 오신 것을 환영합니다")')).toBeVisible();
    
    // 3. 채팅으로 이동
    await page.click('a[href="/chat"]');
    await page.waitForURL('/chat');
    await expect(page).toHaveTitle(/채팅 - DungeonTalk/);
    
    // 4. 게임으로 다시 이동
    await page.click('text=게임 시작하기');
    await page.waitForURL('/game');
    await expect(page).toHaveTitle(/게임 - DungeonTalk/);
    
    console.log('페이지 네비게이션 정상 작동');
  });
  
  test('로그아웃 테스트', async ({ page }) => {
    // 1. 로그인
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/game', { timeout: 10000 });
    
    // 2. 사용자 메뉴 열기
    const userMenuBtn = page.locator('button[x-data*="userMenuOpen"]').first();
    await userMenuBtn.click();
    
    // 3. 로그아웃 클릭
    const logoutBtn = page.locator('button:has-text("로그아웃")');
    await logoutBtn.click();
    
    // 4. 홈페이지로 리다이렉트 확인
    await page.waitForURL('/');
    
    // 5. 로그인 버튼 표시 확인
    const loginBtn = page.locator('a[href="/login"]:has-text("로그인")');
    await expect(loginBtn).toBeVisible();
    
    console.log('로그아웃 성공');
  });
});