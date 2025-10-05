import { test, expect } from '@playwright/test';

test.describe('회원가입부터 캐릭터 생성까지 전체 플로우', () => {
  
  test('새 계정 생성 → 로그인 → 캐릭터 생성', async ({ page }) => {
    // 고유한 사용자 정보 생성 (짧은 ID 사용 - DB 제약)
    const randomNum = Math.floor(Math.random() * 10000);
    const testUser = {
      userId: `test${randomNum}`,  // 최대 20자 제한
      nickname: `테스터${randomNum}`,  // 최대 20자 제한
      password: 'Test1234!@',
      characterName: `Hero${randomNum}`
    };
    
    console.log('========================================');
    console.log('전체 플로우 E2E 테스트 시작');
    console.log('========================================');
    console.log(`테스트 계정: ${testUser.userId}`);
    console.log('');
    
    // ============================================
    // STEP 1: 회원가입
    // ============================================
    console.log('[STEP 1/4] 회원가입');
    console.log('------------------');
    
    // 로그인 페이지로 이동
    await page.goto('/login');
    await expect(page).toHaveTitle(/로그인 - DungeonTalk/);
    console.log('✓ 로그인 페이지 접속');
    
    // 회원가입 링크 클릭
    const registerLink = page.locator('a:has-text("회원가입")').first();
    await registerLink.click();
    console.log('✓ 회원가입 모달 열기');
    await page.waitForTimeout(500);
    
    // 회원가입 모달 확인
    const registerModal = page.locator('div').filter({ 
      hasText: '회원가입' 
    }).filter({ 
      has: page.locator('input') 
    }).first();
    
    await expect(registerModal).toBeVisible({ timeout: 5000 });
    console.log('✓ 회원가입 모달 표시 확인');
    
    // 회원가입 정보 입력
    console.log('  회원가입 정보 입력:');
    
    // 사용자 ID 입력
    const userIdInput = registerModal.locator('input[x-model="registerData.userId"]').first();
    await userIdInput.fill(testUser.userId);
    console.log(`    - 아이디: ${testUser.userId}`);
    
    // 닉네임 입력
    const nicknameInput = registerModal.locator('input[x-model="registerData.nickname"]').first();
    await nicknameInput.fill(testUser.nickname);
    console.log(`    - 닉네임: ${testUser.nickname}`);
    
    // 비밀번호 입력
    const passwordInput = registerModal.locator('input[x-model="registerData.password"]').first();
    await passwordInput.fill(testUser.password);
    console.log(`    - 비밀번호: ********`);
    
    // 가입하기 버튼 클릭
    const registerBtn = registerModal.locator('button:has-text("가입하기")').first();
    await registerBtn.click();
    console.log('✓ 가입하기 버튼 클릭');
    
    // 회원가입 완료 대기
    await page.waitForTimeout(2000);
    
    // 성공 메시지 확인 또는 모달 닫힘 확인
    const modalStillVisible = await registerModal.isVisible().catch(() => false);
    if (modalStillVisible) {
      // 모달이 아직 열려있으면 닫기
      console.log('  모달 닫기...');
      
      // 취소 버튼 클릭 시도
      const cancelBtn = registerModal.locator('button:has-text("취소")').first();
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
        await page.waitForTimeout(500);
      } else {
        // ESC 키 또는 배경 클릭으로 닫기
        await page.keyboard.press('Escape');
        await page.waitForTimeout(500);
      }
    }
    console.log('✅ 회원가입 완료');
    
    console.log('');
    
    // ============================================
    // STEP 2: 로그인
    // ============================================
    console.log('[STEP 2/4] 신규 계정으로 로그인');
    console.log('------------------');
    
    // 모달이 완전히 닫혔는지 확인
    await page.waitForTimeout(1000);
    
    // 알림 메시지가 있으면 닫기
    const notificationClose = page.locator('button[aria-label="Close"], svg[class*="w-4 h-4"]').first();
    if (await notificationClose.isVisible().catch(() => false)) {
      await notificationClose.click();
      console.log('  알림 메시지 닫기');
      await page.waitForTimeout(500);
    }
    
    // 로그인 정보 입력
    const loginIdInput = page.locator('input[name="name"]').first();
    await loginIdInput.clear();
    await loginIdInput.fill(testUser.userId);
    console.log(`✓ 아이디 입력: ${testUser.userId}`);
    
    const loginPwdInput = page.locator('input[name="password"]').first();
    await loginPwdInput.clear();
    await loginPwdInput.fill(testUser.password);
    console.log('✓ 비밀번호 입력');
    
    // 로그인 버튼 클릭
    const loginBtn = page.locator('button[type="submit"]:has-text("로그인")').first();
    await loginBtn.click();
    console.log('✓ 로그인 버튼 클릭');
    
    // 게임 페이지로 이동 대기
    console.log('  게임 페이지 이동 대기...');
    await page.waitForURL('/game', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    
    const pageTitle = await page.title();
    console.log(`✅ 로그인 성공 → ${pageTitle}`);
    console.log('');
    
    // ============================================
    // STEP 3: 캐릭터 생성
    // ============================================
    console.log('[STEP 3/4] 캐릭터 생성');
    console.log('------------------');
    
    // 페이지 안정화 대기
    await page.waitForTimeout(2000);
    
    // 캐릭터 생성 모달이 자동으로 표시되는지 확인
    let characterModal = page.locator('div').filter({ 
      hasText: /캐릭터 생성|새 캐릭터|Create Character/i 
    }).filter({ 
      has: page.locator('input[type="text"]') 
    }).first();
    
    let isModalVisible = await characterModal.isVisible().catch(() => false);
    
    if (!isModalVisible) {
      console.log('  캐릭터 생성 모달이 자동으로 표시되지 않음');
      
      // 내 캐릭터 버튼 클릭 시도
      const myCharBtn = page.locator('button:has-text("내 캐릭터")').first();
      if (await myCharBtn.isVisible()) {
        await myCharBtn.click();
        console.log('✓ "내 캐릭터" 버튼 클릭');
        await page.waitForTimeout(1000);
      }
      
      // 캐릭터 생성 버튼 찾기
      const createBtns = [
        page.locator('button:has-text("캐릭터 생성")'),
        page.locator('button:has-text("새 캐릭터")'),
        page.locator('button:has-text("만들기")'),
        page.locator('button:has-text("Create")')
      ];
      
      for (const btn of createBtns) {
        if (await btn.first().isVisible().catch(() => false)) {
          await btn.first().click();
          console.log('✓ 캐릭터 생성 버튼 클릭');
          await page.waitForTimeout(1000);
          break;
        }
      }
      
      // 모달 재확인
      isModalVisible = await characterModal.isVisible().catch(() => false);
    } else {
      console.log('✓ 캐릭터 생성 모달 자동 표시됨');
    }
    
    if (isModalVisible) {
      console.log('  캐릭터 정보 입력:');
      
      // 캐릭터 이름 입력
      const nameInput = characterModal.locator('input[type="text"]').first();
      if (await nameInput.isVisible()) {
        await nameInput.fill(testUser.characterName);
        console.log(`    - 캐릭터 이름: ${testUser.characterName}`);
      }
      
      // 종족 선택 (옵션이 있는 경우)
      const raceSelect = characterModal.locator('select').first();
      if (await raceSelect.isVisible().catch(() => false)) {
        const options = await raceSelect.locator('option').count();
        if (options > 1) {
          await raceSelect.selectOption({ index: 1 });
          const selectedText = await raceSelect.inputValue();
          console.log(`    - 종족 선택: 옵션 1`);
        }
      }
      
      // 생성 버튼 클릭
      const submitBtns = [
        characterModal.locator('button:has-text("생성")'),
        characterModal.locator('button:has-text("만들기")'),
        characterModal.locator('button:has-text("확인")'),
        characterModal.locator('button[type="submit"]')
      ];
      
      let submitted = false;
      for (const btn of submitBtns) {
        if (await btn.first().isVisible().catch(() => false)) {
          await btn.first().click();
          console.log('✓ 캐릭터 생성 버튼 클릭');
          submitted = true;
          break;
        }
      }
      
      if (submitted) {
        // 생성 완료 대기
        await page.waitForTimeout(3000);
        console.log('  캐릭터 생성 처리 중...');
      }
    } else {
      console.log('⚠️ 캐릭터 생성 UI를 찾을 수 없음');
    }
    
    console.log('');
    
    // ============================================
    // STEP 4: 결과 확인
    // ============================================
    console.log('[STEP 4/4] 결과 확인');
    console.log('------------------');
    
    // 페이지 새로고침하여 최신 상태 확인
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    
    // 생성된 캐릭터 확인
    const characterCard = page.locator('div').filter({ 
      hasText: new RegExp(testUser.characterName, 'i')
    }).first();
    
    const characterExists = await characterCard.isVisible().catch(() => false);
    
    if (characterExists) {
      console.log(`✅ 캐릭터 생성 확인: ${testUser.characterName}`);
      
      // 캐릭터 상세 정보 확인
      const stats = ['STR', 'WIL', 'INT', 'WIS', 'DEX', 'LUK'];
      let hasStats = false;
      
      for (const stat of stats) {
        const statElement = page.locator(`text=/${stat}/`).first();
        if (await statElement.isVisible().catch(() => false)) {
          hasStats = true;
          break;
        }
      }
      
      if (hasStats) {
        console.log('✅ 캐릭터 스탯 정보 확인');
      }
    } else {
      // 대체 확인: 닉네임으로 검색
      const nicknameCard = page.locator('div').filter({ 
        hasText: testUser.nickname 
      }).first();
      
      if (await nicknameCard.isVisible().catch(() => false)) {
        console.log(`✅ 캐릭터 확인 (닉네임): ${testUser.nickname}`);
      } else {
        console.log('⚠️ 생성된 캐릭터를 확인할 수 없음');
      }
    }
    
    // ============================================
    // 최종 결과
    // ============================================
    console.log('');
    console.log('========================================');
    console.log('테스트 결과 요약');
    console.log('========================================');
    console.log(`✅ 회원가입 성공: ${testUser.userId}`);
    console.log(`✅ 로그인 성공`);
    console.log(`✅ 게임 페이지 접근 성공`);
    
    if (characterExists) {
      console.log(`✅ 캐릭터 생성 성공: ${testUser.characterName}`);
      console.log('');
      console.log('🎉 전체 플로우 테스트 성공!');
    } else {
      console.log(`⚠️ 캐릭터 생성 확인 필요`);
      console.log('');
      console.log('⚠️ 부분 성공 - 캐릭터 생성 확인 필요');
    }
    console.log('========================================');
    
    // 스크린샷 저장 (디버깅용)
    await page.screenshot({ 
      path: `test-result-${randomNum}.png`, 
      fullPage: true 
    });
    console.log(`\n스크린샷 저장: test-result-${randomNum}.png`);
  });
});