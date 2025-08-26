// 테마 및 세계관 UI 관리
export class ThemeManager {
    constructor() {
        this.selectedWorld = null;
    }

    // 세계관 선택
    selectWorld(worldType) {
        // 이전 선택 해제
        document.querySelectorAll('.world-card').forEach(card => {
            card.classList.remove('selected');
        });
        
        // 새로운 선택
        const selectedCard = document.querySelector(`[data-world="${worldType}"]`);
        if (selectedCard) {
            selectedCard.classList.add('selected');
        }
        
        this.selectedWorld = worldType;
        
        // 배경 테마 적용
        this.applyWorldTheme(worldType);
        
        // 세계관 정보 표시
        this.updateWorldInfo(worldType);
        
        // 매칭 시작 버튼 활성화
        const startMatchingBtn = document.getElementById('startMatchingBtn');
        if (startMatchingBtn) {
            startMatchingBtn.disabled = false;
        }
        
        console.log(`✨ ${this.getWorldName(worldType)} 세계관 선택됨`);
        return worldType;
    }

    getWorldName(worldType) {
        const names = {
            'FANTASY': '🏰 판타지',
            'ZOMBIE': '🧟 좀비'
        };
        return names[worldType] || worldType;
    }

    // 세계관 테마 적용
    applyWorldTheme(worldType) {
        // 기존 테마 클래스 제거
        document.body.classList.remove('theme-fantasy', 'theme-zombie');
        
        // 새 테마 클래스 추가
        switch(worldType) {
            case 'FANTASY':
                document.body.classList.add('theme-fantasy');
                this.updateAiChatInfo('🏰 중세 판타지 세계에서의 모험이 시작됩니다! 마법과 검의 세계에서 용감한 행동을 취해보세요.');
                this.updatePlaceholders('마법사에게 말을 걸어본다', '동료들과 전략을 논의해보세요');
                break;
            case 'ZOMBIE':
                document.body.classList.add('theme-zombie');
                this.updateAiChatInfo('🧟 좀비 아포칼립스 세계에서 살아남으세요! 생존을 위해 자원을 찾고 위험을 피하세요.');
                this.updatePlaceholders('주변에서 물품을 찾아본다', '생존자들과 계획을 세우세요');
                break;
            default:
                console.warn('알 수 없는 세계관 타입:', worldType);
        }
    }

    // 세계관 정보 업데이트
    updateWorldInfo(worldType) {
        const worldInfoCard = document.getElementById('worldInfoCard');
        const selectedWorldInfo = document.getElementById('selectedWorldInfo');
        
        if (worldInfoCard) {
            worldInfoCard.style.display = 'block';
        }
        if (selectedWorldInfo) {
            selectedWorldInfo.textContent = this.getWorldName(worldType);
        }
    }

    // AI 채팅 정보 업데이트
    updateAiChatInfo(message) {
        const aiChatInfo = document.getElementById('aiChatInfo');
        if (aiChatInfo) {
            aiChatInfo.textContent = message;
        }
    }

    // 플레이스홀더 업데이트
    updatePlaceholders(aiPlaceholder, userPlaceholder) {
        const aiInput = document.getElementById('aiMessageInput');
        const rightUserInput = document.getElementById('rightUserMessageInput');
        
        if (aiInput) {
            aiInput.placeholder = `${aiPlaceholder} (예시 행동)`;
        }
        if (rightUserInput) {
            rightUserInput.placeholder = userPlaceholder;
        }
    }

    // 세계관별 환영 메시지 생성
    getWorldSpecificWelcomeMessages(worldType) {
        const messages = [];
        
        switch(worldType) {
            case 'FANTASY':
                messages.push({
                    type: 'system',
                    content: '🌟 중세 판타지 세계에 오신 것을 환영합니다!'
                });
                messages.push({
                    type: 'system',
                    content: '⚔️ 마법과 검의 세계에서 전설적인 모험을 시작하세요!'
                });
                messages.push({
                    type: 'ai',
                    content: '안녕하세요, 용감한 모험가들이여! 저는 여러분의 운명을 안내하는 고대의 지식을 가진 AI 오라클입니다. 이 신비로운 대륙에서 여러분의 영웅적인 여정이 시작됩니다. 먼저 자신을 소개하고, 어떤 모험을 찾고 있는지 말해주세요.'
                });
                break;
            case 'ZOMBIE':
                messages.push({
                    type: 'system',
                    content: '🧟 좀비 아포칼립스 세계에 오신 것을 환영합니다!'
                });
                messages.push({
                    type: 'system',
                    content: '🔫 생존이 최우선인 위험한 세계입니다!'
                });
                messages.push({
                    type: 'ai',
                    content: '안녕하세요, 생존자들이여. 저는 이 황폐한 세계에서 여러분의 생존을 도울 AI 가이드입니다. 바이러스로 인해 세상이 바뀐 지 이제 몇 개월이 흘렀습니다. 먼저 각자 자신이 누구였는지, 그리고 어떻게 여기까지 살아남았는지 말해주세요.'
                });
                break;
            default:
                messages.push({
                    type: 'system',
                    content: `🌟 ${this.getWorldName(worldType)} 세계에 오신 것을 환영합니다!`
                });
                messages.push({
                    type: 'system',
                    content: '🤖 AI 마스터가 여러분의 모험을 안내할 것입니다.'
                });
                messages.push({
                    type: 'ai',
                    content: `안녕하세요 모험가들! 저는 여러분의 게임 마스터 AI입니다. ${this.getWorldName(worldType)} 세계에서 펼쳐진 모험을 함께 만들어갑시다. 먼저 자기소개를 해주세요!`
                });
        }
        
        return messages;
    }

    // 테마 초기화
    resetTheme() {
        // 모든 테마 클래스 제거
        document.body.classList.remove('theme-fantasy', 'theme-zombie');
        
        // 세계관 선택 초기화
        document.querySelectorAll('.world-card').forEach(card => {
            card.classList.remove('selected');
        });
        
        this.selectedWorld = null;
        
        // 세계관 정보 숨기기
        const worldInfoCard = document.getElementById('worldInfoCard');
        if (worldInfoCard) {
            worldInfoCard.style.display = 'none';
        }
        
        // 매칭 버튼 비활성화
        const startMatchingBtn = document.getElementById('startMatchingBtn');
        if (startMatchingBtn) {
            startMatchingBtn.disabled = true;
        }
        
        // 기본 플레이스홀더로 복원
        this.updatePlaceholders('행동을 입력하세요', '동료들과 대화하세요');
        
        // AI 채팅 정보 초기화
        this.updateAiChatInfo('세계관을 선택하고 게임을 시작하세요!');
    }

    // getter methods
    getSelectedWorld() {
        return this.selectedWorld;
    }

    // 전역 함수 바인딩
    bindGlobalFunctions() {
        window.selectWorld = (worldType) => this.selectWorld(worldType);
    }
}