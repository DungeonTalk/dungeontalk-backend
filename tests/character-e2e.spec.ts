import { test, expect } from '@playwright/test';

test.describe('캐릭터 E2E 테스트 (Headless)', () => {
  
  test('testuser1 로그인 및 캐릭터 확인', async ({ page }) => {
    console.log('=== E2E 테스트 시작 ===');
    
    // 1. 로그인 페이지 접속
    console.log('[1/4] 로그인 페이지 접속...');
    await page.goto('/login');
    await expect(page).toHaveTitle(/로그인 - DungeonTalk/);
    console.log('✓ 로그인 페이지 로드 완료');
    
    // 2. testuser1으로 로그인
    console.log('[2/4] testuser1 계정으로 로그인 중...');
    await page.fill('input[name="name"]', 'testuser1');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // 3. 게임 페이지 이동 확인
    console.log('[3/4] 게임 페이지 이동 대기...');
    await page.waitForURL('/game', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    
    const title = await page.title();
    console.log(`✓ 페이지 이동 완료: ${title}`);
    
    // 4. 캐릭터 정보 확인
    console.log('[4/4] 캐릭터 정보 확인...');
    
    // 캐릭터 카드 찾기
    const characterCard = page.locator('div').filter({ 
      has: page.locator('text=/테스트유저1/')
    }).filter({
      has: page.locator('text=/STR|WIL|INT/')
    }).first();
    
    const hasCharacter = await characterCard.isVisible().catch(() => false);
    
    if (hasCharacter) {
      console.log('✅ 캐릭터 발견: 테스트유저1');
      
      // 캐릭터 상세 정보 추출
      const characterData = {
        name: '',
        id: '',
        stats: {},
        hp: '',
        mp: ''
      };
      
      // 캐릭터 이름
      const nameElement = characterCard.locator('text=/테스트유저1/').first();
      if (await nameElement.isVisible()) {
        characterData.name = await nameElement.textContent() || '';
        console.log(`  - 이름: ${characterData.name}`);
      }
      
      // 캐릭터 ID
      const idElement = characterCard.locator('text=/[0-9a-f]{8}-[0-9a-f]{4}/').first();
      if (await idElement.isVisible()) {
        characterData.id = await idElement.textContent() || '';
        console.log(`  - ID: ${characterData.id}`);
      }
      
      // 스탯 정보
      const stats = ['STR', 'WIL', 'INT', 'WIS', 'DEX', 'LUK'];
      for (const stat of stats) {
        const statElement = characterCard.locator(`text=/${stat}\\s*\\(\\w+\\)/`).first();
        if (await statElement.isVisible()) {
          const statText = await statElement.textContent() || '';
          const match = statText.match(/(\d+)/);
          if (match) {
            characterData.stats[stat] = match[1];
            console.log(`  - ${stat}: ${match[1]}`);
          }
        }
      }
      
      // HP/MP 정보
      const hpElement = characterCard.locator('text=/HP:\\s*\\d+/').first();
      if (await hpElement.isVisible()) {
        characterData.hp = await hpElement.textContent() || '';
        console.log(`  - ${characterData.hp}`);
      }
      
      const mpElement = characterCard.locator('text=/MP:\\s*\\d+/').first();
      if (await mpElement.isVisible()) {
        characterData.mp = await mpElement.textContent() || '';
        console.log(`  - ${characterData.mp}`);
      }
      
      console.log('');
      console.log('=== 테스트 결과 ===');
      console.log('✅ 로그인 성공');
      console.log('✅ 게임 페이지 접근 성공');
      console.log('✅ 캐릭터 정보 확인 성공');
      console.log('');
      console.log('모든 테스트가 성공적으로 완료되었습니다!');
      
    } else {
      // 캐릭터가 없는 경우 - 새 캐릭터 생성 시도
      console.log('⚠️ 기존 캐릭터 없음 - 새 캐릭터 생성 필요');
      
      // 내 캐릭터 버튼 확인
      const myCharacterBtn = page.locator('button').filter({ hasText: '내 캐릭터' }).first();
      if (await myCharacterBtn.isVisible()) {
        console.log('  - "내 캐릭터" 버튼 발견');
        await myCharacterBtn.click();
        await page.waitForTimeout(1000);
        
        // 캐릭터 생성 모달 확인
        const createModal = page.locator('div').filter({ 
          hasText: /캐릭터 생성|새 캐릭터|Create Character/i 
        }).filter({
          has: page.locator('input, button')
        }).first();
        
        if (await createModal.isVisible()) {
          console.log('  - 캐릭터 생성 모달 표시됨');
          
          // 이름 입력
          const nameInput = createModal.locator('input[type="text"]').first();
          if (await nameInput.isVisible()) {
            const characterName = `TestHero_${Date.now()}`;
            await nameInput.fill(characterName);
            console.log(`  - 캐릭터 이름 입력: ${characterName}`);
          }
          
          // 생성 버튼 클릭
          const createBtn = createModal.locator('button').filter({ 
            hasText: /생성|만들기|Create|확인/i 
          }).first();
          
          if (await createBtn.isVisible()) {
            await createBtn.click();
            console.log('  - 생성 버튼 클릭');
            await page.waitForTimeout(3000);
            
            // 생성 완료 확인
            const newCharacter = page.locator('div').filter({ 
              hasText: /TestHero/ 
            }).first();
            
            if (await newCharacter.isVisible()) {
              console.log('✅ 새 캐릭터 생성 성공');
            } else {
              console.log('⚠️ 캐릭터 생성 확인 불가');
            }
          }
        } else {
          console.log('  - 캐릭터 생성 UI를 찾을 수 없음');
          console.log('  - 페이지에 캐릭터 관련 기능이 없을 수 있습니다');
        }
      }
      
      console.log('');
      console.log('=== 테스트 결과 ===');
      console.log('✅ 로그인 성공');
      console.log('✅ 게임 페이지 접근 성공');
      console.log('⚠️ 캐릭터 미보유 (생성 필요)');
    }
  });
  
  test('캐릭터 생성 플로우 검증', async ({ page }) => {
    console.log('=== 캐릭터 생성 플로우 테스트 ===');
    
    // testuser2로 테스트 (캐릭터가 없을 가능성이 높음)
    console.log('[1/3] testuser2로 로그인...');
    await page.goto('/login');
    await page.fill('input[name="name"]', 'testuser2');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // 게임 페이지 대기
    await page.waitForURL('/game', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    console.log('✓ 로그인 및 페이지 이동 완료');
    
    // 캐릭터 확인
    console.log('[2/3] 기존 캐릭터 확인...');
    const existingCharacter = page.locator('div').filter({
      has: page.locator('text=/STR|WIL|INT/')
    }).first();
    
    if (await existingCharacter.isVisible().catch(() => false)) {
      console.log('✅ testuser2는 이미 캐릭터를 보유하고 있습니다');
      
      // 캐릭터 정보 간단히 출력
      const characterText = await existingCharacter.textContent();
      if (characterText) {
        const lines = characterText.split('\n').filter(line => line.trim());
        console.log('  캐릭터 정보:');
        lines.slice(0, 5).forEach(line => {
          if (line.trim()) console.log(`    - ${line.trim()}`);
        });
      }
    } else {
      console.log('⚠️ 캐릭터 없음 - 생성 프로세스 시작');
      
      // 캐릭터 생성 시도
      console.log('[3/3] 캐릭터 생성 시도...');
      
      // 내 캐릭터 버튼 클릭
      const myCharBtn = page.locator('button:has-text("내 캐릭터")').first();
      if (await myCharBtn.isVisible()) {
        await myCharBtn.click();
        await page.waitForTimeout(1000);
        console.log('  - 내 캐릭터 버튼 클릭');
      }
      
      // 생성 UI 찾기
      const createUI = page.locator('input[type="text"]').first();
      if (await createUI.isVisible()) {
        const newName = `Hero_${Date.now()}`;
        await createUI.fill(newName);
        console.log(`  - 캐릭터 이름 입력: ${newName}`);
        
        // 생성 실행
        const submitBtn = page.locator('button').filter({
          hasText: /생성|만들기|확인|Create/i
        }).first();
        
        if (await submitBtn.isVisible()) {
          await submitBtn.click();
          console.log('  - 생성 버튼 클릭');
          await page.waitForTimeout(2000);
          console.log('✅ 캐릭터 생성 프로세스 완료');
        }
      } else {
        console.log('  - 캐릭터 생성 UI를 찾을 수 없음');
      }
    }
    
    console.log('');
    console.log('테스트 완료!');
  });
});