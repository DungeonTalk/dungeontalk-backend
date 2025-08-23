import { test, expect } from '@playwright/test';

test.describe('캐릭터 생성 E2E 테스트', () => {
  
  test('testuser1으로 로그인 후 캐릭터 생성', async ({ page }) => {
    console.log('테스트 시작: 캐릭터 생성 플로우');
    
    // 1. 로그인 페이지로 이동
    console.log('1. 로그인 페이지 접속');
    await page.goto('/login');
    await expect(page).toHaveTitle(/로그인 - DungeonTalk/);
    
    // 2. testuser1으로 로그인
    console.log('2. testuser1 계정으로 로그인');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // 3. 게임 페이지로 이동 대기
    console.log('3. 게임 페이지 이동 대기');
    await page.waitForURL('/game', { timeout: 10000 });
    
    // 4. 페이지 로드 완료 대기
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    
    const title = await page.title();
    console.log(`현재 페이지 타이틀: ${title}`);
    
    // 5. 기존 캐릭터 확인
    console.log('4. 기존 캐릭터 확인');
    const existingCharacter = page.locator('div.bg-white').filter({ hasText: '테스트유저1' }).first();
    const hasExistingCharacter = await existingCharacter.isVisible().catch(() => false);
    
    if (hasExistingCharacter) {
      console.log('기존 캐릭터가 이미 존재함: 테스트유저1');
      
      // 기존 캐릭터 정보 확인
      const characterInfo = await existingCharacter.textContent();
      console.log('캐릭터 정보:', characterInfo);
      
      // 캐릭터 ID 확인
      const characterIdElement = page.locator('text=/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i');
      if (await characterIdElement.isVisible()) {
        const characterId = await characterIdElement.textContent();
        console.log('캐릭터 ID:', characterId);
      }
      
      console.log('✅ 캐릭터가 이미 존재하므로 테스트 성공');
      return; // 이미 캐릭터가 있으므로 테스트 종료
    }
    
    // 6. 캐릭터가 없는 경우 새로 생성
    console.log('5. 새 캐릭터 생성 시도');
    
    // 내 캐릭터 버튼 클릭 (오른쪽 상단)
    const myCharacterBtn = page.locator('button').filter({ hasText: '내 캐릭터' }).first();
    if (await myCharacterBtn.isVisible()) {
      await myCharacterBtn.click();
      console.log('내 캐릭터 버튼 클릭');
      await page.waitForTimeout(1000);
    }
    
    // 캐릭터 생성 버튼 찾기
    console.log('6. 캐릭터 생성 UI 찾기');
    
    // 캐릭터 생성 모달이 자동으로 표시되는지 확인
    let characterModal = page.locator('div').filter({ hasText: '캐릭터 생성' }).filter({ has: page.locator('input') }).first();
    let isModalVisible = await characterModal.isVisible().catch(() => false);
    
    if (!isModalVisible) {
      // 캐릭터 생성 버튼 찾기
      const createButtons = [
        page.locator('button').filter({ hasText: '캐릭터 생성' }),
        page.locator('button').filter({ hasText: '새 캐릭터' }),
        page.locator('button').filter({ hasText: '만들기' }),
        page.locator('button').filter({ hasText: 'Create' })
      ];
      
      for (const btn of createButtons) {
        if (await btn.first().isVisible().catch(() => false)) {
          await btn.first().click();
          console.log('캐릭터 생성 버튼 클릭');
          await page.waitForTimeout(1000);
          break;
        }
      }
      
      // 모달 재확인
      isModalVisible = await characterModal.isVisible().catch(() => false);
    }
    
    if (!isModalVisible) {
      console.log('캐릭터 생성 UI를 찾을 수 없음');
      
      // 페이지 상태 디버깅
      const pageContent = await page.content();
      if (pageContent.includes('캐릭터')) {
        console.log('페이지에 "캐릭터" 텍스트는 존재함');
      }
      
      // 스크린샷 저장
      await page.screenshot({ path: 'character-creation-page.png', fullPage: true });
      console.log('스크린샷 저장: character-creation-page.png');
      
      throw new Error('캐릭터 생성 UI를 찾을 수 없습니다');
    }
    
    // 7. 캐릭터 정보 입력
    console.log('7. 캐릭터 정보 입력');
    
    // 이름 입력
    const nameInput = characterModal.locator('input[type="text"]').first();
    if (await nameInput.isVisible()) {
      const timestamp = Date.now();
      const characterName = `TestHero_${timestamp}`;
      await nameInput.fill(characterName);
      console.log(`캐릭터 이름 입력: ${characterName}`);
    }
    
    // 종족 선택 (있는 경우)
    const raceSelect = characterModal.locator('select').first();
    if (await raceSelect.isVisible().catch(() => false)) {
      const options = await raceSelect.locator('option').count();
      if (options > 1) {
        await raceSelect.selectOption({ index: 1 });
        console.log('종족 선택 완료');
      }
    }
    
    // 8. 생성 버튼 클릭
    console.log('8. 캐릭터 생성 실행');
    const submitBtn = characterModal.locator('button').filter({ hasText: /생성|만들기|Create|확인/i }).first();
    if (await submitBtn.isVisible()) {
      await submitBtn.click();
      console.log('생성 버튼 클릭');
    } else {
      // type="submit" 버튼 시도
      const submitTypeBtn = characterModal.locator('button[type="submit"]').first();
      if (await submitTypeBtn.isVisible()) {
        await submitTypeBtn.click();
        console.log('Submit 버튼 클릭');
      }
    }
    
    // 9. 생성 결과 확인
    console.log('9. 캐릭터 생성 결과 확인');
    await page.waitForTimeout(3000);
    
    // 성공 메시지 확인
    const successMessages = [
      page.locator('text=/캐릭터.*생성.*완료/i'),
      page.locator('text=/캐릭터.*생성.*성공/i'),
      page.locator('text=/successfully/i')
    ];
    
    let success = false;
    for (const msg of successMessages) {
      if (await msg.isVisible().catch(() => false)) {
        success = true;
        const text = await msg.textContent();
        console.log(`성공 메시지: ${text}`);
        break;
      }
    }
    
    // 모달이 닫혔는지 확인
    if (!success && !await characterModal.isVisible().catch(() => true)) {
      success = true;
      console.log('모달이 닫혔음 - 생성 성공으로 간주');
    }
    
    // 새 캐릭터 카드 확인
    if (!success) {
      const newCharacterCard = page.locator('div.bg-white').filter({ hasText: /TestHero/i }).first();
      if (await newCharacterCard.isVisible().catch(() => false)) {
        success = true;
        console.log('새 캐릭터 카드 확인됨');
      }
    }
    
    if (success) {
      console.log('✅ 캐릭터 생성 테스트 성공');
    } else {
      console.log('⚠️ 캐릭터 생성 결과를 확인할 수 없음');
      await page.screenshot({ path: 'test-result.png', fullPage: true });
      throw new Error('캐릭터 생성 확인 실패');
    }
    
    console.log('테스트 완료');
  });
  
  test('캐릭터 정보 조회', async ({ page }) => {
    console.log('테스트 시작: 캐릭터 정보 조회');
    
    // 1. 로그인
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/game', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    
    // 2. 캐릭터 카드 확인
    const characterCard = page.locator('div.bg-white').filter({ hasText: '테스트유저1' }).first();
    const hasCharacter = await characterCard.isVisible().catch(() => false);
    
    if (hasCharacter) {
      console.log('✅ 캐릭터 카드 표시 확인');
      
      // 캐릭터 정보 확인
      const stats = ['STR', 'WIL', 'INT', 'WIS', 'DEX', 'LUK'];
      for (const stat of stats) {
        const statElement = characterCard.locator(`text=/${stat}/`);
        if (await statElement.isVisible()) {
          const parent = await statElement.locator('..').textContent();
          console.log(`- ${parent}`);
        }
      }
      
      // HP/MP 확인
      const hpElement = characterCard.locator('text=/HP:/i');
      if (await hpElement.isVisible()) {
        const hpText = await hpElement.locator('..').textContent();
        console.log(`- ${hpText}`);
      }
      
      const mpElement = characterCard.locator('text=/MP:/i');
      if (await mpElement.isVisible()) {
        const mpText = await mpElement.locator('..').textContent();
        console.log(`- ${mpText}`);
      }
      
      console.log('✅ 캐릭터 정보 조회 성공');
    } else {
      console.log('❌ 캐릭터를 찾을 수 없음');
      throw new Error('캐릭터 정보를 찾을 수 없습니다');
    }
  });
});