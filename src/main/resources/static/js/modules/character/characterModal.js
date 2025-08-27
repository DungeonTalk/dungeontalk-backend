// 캐릭터 모달 UI 관리
export class CharacterModal {
    constructor(characterService) {
        this.characterService = characterService;
        this.setupEventListeners();
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        // 모달 외부 클릭 시 닫기
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                this.hideAllModals();
            }
        });
    }

    // === 캐릭터 생성 모달 관련 메서드 ===

    // 캐릭터 생성 모달 표시
    async showCharacterCreationModal() {
        try {
            // 종족 목록 로드
            await this.loadRaces();
            
            // 모달 표시
            const modal = document.getElementById('characterCreationModal');
            if (modal) {
                modal.style.display = 'flex';
            }
        } catch (error) {
            console.error('캐릭터 생성 모달 표시 오류:', error);
            alert('종족 정보를 불러오는데 실패했습니다.');
        }
    }

    // 캐릭터 생성 모달 숨기기
    hideCharacterCreationModal() {
        const modal = document.getElementById('characterCreationModal');
        if (modal) {
            modal.style.display = 'none';
        }
        
        const raceSelect = document.getElementById('raceSelect');
        if (raceSelect) {
            raceSelect.value = '';
        }
        
        const raceDescription = document.getElementById('raceDescription');
        if (raceDescription) {
            raceDescription.style.display = 'none';
        }
        
        const createCharacterBtn = document.getElementById('createCharacterBtn');
        if (createCharacterBtn) {
            createCharacterBtn.disabled = true;
        }
    }

    // 종족 목록 로드
    async loadRaces() {
        try {
            const result = await this.characterService.loadRaces();
            
            const raceSelect = document.getElementById('raceSelect');
            if (!raceSelect) {
                throw new Error('raceSelect 엘리먼트를 찾을 수 없습니다.');
            }
            
            // 기존 옵션 제거 (첫 번째 옵션 제외)
            while (raceSelect.children.length > 1) {
                raceSelect.removeChild(raceSelect.lastChild);
            }
            
            // 종족 옵션 추가
            result.races.forEach(raceName => {
                const option = document.createElement('option');
                option.value = raceName;
                option.textContent = raceName;
                raceSelect.appendChild(option);
            });
            
            // 종족 선택 이벤트 리스너 추가 (기존 리스너 제거 후 추가)
            const newRaceSelect = raceSelect.cloneNode(true);
            raceSelect.parentNode.replaceChild(newRaceSelect, raceSelect);
            
            newRaceSelect.addEventListener('change', (e) => {
                const selectedRace = e.target.value;
                if (selectedRace) {
                    this.showRaceDescription(selectedRace);
                    const createBtn = document.getElementById('createCharacterBtn');
                    if (createBtn) createBtn.disabled = false;
                } else {
                    const raceDescription = document.getElementById('raceDescription');
                    if (raceDescription) raceDescription.style.display = 'none';
                    const createBtn = document.getElementById('createCharacterBtn');
                    if (createBtn) createBtn.disabled = true;
                }
            });
            
        } catch (error) {
            console.error('종족 목록 로드 오류:', error);
            throw error;
        }
    }

    // 종족 설명 표시
    showRaceDescription(raceName) {
        const descriptions = this.characterService.getRaceDescriptions();
        const desc = descriptions[raceName];
        
        if (desc) {
            const titleElement = document.getElementById('raceDescTitle');
            const contentElement = document.getElementById('raceDescContent');
            const descriptionElement = document.getElementById('raceDescription');
            
            if (titleElement) titleElement.textContent = desc.title;
            if (contentElement) contentElement.textContent = desc.content;
            if (descriptionElement) descriptionElement.style.display = 'block';
        }
    }

    // 캐릭터 생성
    async createCharacter() {
        const raceSelect = document.getElementById('raceSelect');
        const selectedRace = raceSelect ? raceSelect.value : '';
        
        if (!selectedRace) {
            alert('종족을 선택해주세요.');
            return;
        }
        
        try {
            const result = await this.characterService.createCharacter(selectedRace);
            
            // 모달 숨기기
            this.hideCharacterCreationModal();
            
            // 성공 메시지
            alert(result.message);
            
            return result;
            
        } catch (error) {
            console.error('캐릭터 생성 오류:', error);
            alert(error.message);
        }
    }

    // === 내 캐릭터 모달 관련 메서드 ===

    // 내 캐릭터 모달 표시
    async showMyCharacterModal() {
        try {
            const currentUser = this.characterService.auth.getCurrentUser();
            const hasCharacter = await this.characterService.checkCharacterExists(currentUser.id);
            
            const modal = document.getElementById('myCharacterModal');
            if (!modal) {
                console.error('myCharacterModal 엘리먼트를 찾을 수 없습니다.');
                return;
            }
            
            if (hasCharacter) {
                // 캐릭터 상세 정보 로드
                await this.loadMyCharacterDetails();
                
                const noCharacterSection = document.getElementById('noCharacterSection');
                const hasCharacterSection = document.getElementById('hasCharacterSection');
                
                if (noCharacterSection) noCharacterSection.style.display = 'none';
                if (hasCharacterSection) hasCharacterSection.style.display = 'block';
            } else {
                // 캐릭터 없음 섹션 표시
                const hasCharacterSection = document.getElementById('hasCharacterSection');
                const noCharacterSection = document.getElementById('noCharacterSection');
                
                if (hasCharacterSection) hasCharacterSection.style.display = 'none';
                if (noCharacterSection) noCharacterSection.style.display = 'block';
            }
            
            modal.style.display = 'flex';
        } catch (error) {
            console.error('내 캐릭터 모달 표시 오류:', error);
            alert('캐릭터 정보를 불러오는데 실패했습니다.');
        }
    }

    // 내 캐릭터 모달 숨기기
    hideMyCharacterModal() {
        const modal = document.getElementById('myCharacterModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    // 내 캐릭터 상세 정보 로드
    async loadMyCharacterDetails() {
        try {
            const result = await this.characterService.loadMyCharacterDetails();
            
            // UI 업데이트
            this.updateCharacterUI(result.basicInfo, result.detailInfo);
            
        } catch (error) {
            console.error('캐릭터 상세 정보 로드 오류:', error);
            throw error;
        }
    }

    // 캐릭터 UI 업데이트
    updateCharacterUI(basicInfo, detailInfo) {
        // 기본 정보
        this.updateElement('charRace', detailInfo.raceName || basicInfo.raceId);
        this.updateElement('charLevel', basicInfo.playerLevel);
        this.updateElement('charExp', basicInfo.totalExp.toLocaleString());
        this.updateElement('charUnspentPoints', basicInfo.unspentPoints);
        
        // 기본 스탯
        this.updateElement('charStrength', basicInfo.strength);
        this.updateElement('charWillpower', basicInfo.willpower);
        this.updateElement('charIntelligence', basicInfo.intelligence);
        this.updateElement('charWisdom', basicInfo.wisdom);
        this.updateElement('charDexterity', basicInfo.dexterity);
        this.updateElement('charLuck', basicInfo.luck);
        
        // 계산된 스탯
        this.updateCalculatedStats(detailInfo.calculatedStats);
    }

    // 계산된 스탯 업데이트
    updateCalculatedStats(calculatedStats) {
        const calculatedStatsContainer = document.getElementById('calculatedStats');
        if (!calculatedStatsContainer) {
            console.warn('calculatedStats 컨테이너를 찾을 수 없습니다.');
            return;
        }
        
        calculatedStatsContainer.innerHTML = '';
        
        if (calculatedStats) {
            const statDisplayNames = this.characterService.getStatDisplayNames();
            
            Object.entries(calculatedStats).forEach(([statKey, value]) => {
                const displayName = statDisplayNames[statKey] || statKey;
                const statElement = document.createElement('div');
                statElement.className = 'calculated-stat-item';
                statElement.innerHTML = `
                    <div class="stat-name">${displayName}</div>
                    <div class="stat-value">${typeof value === 'number' ? value.toFixed(1) : value}</div>
                `;
                calculatedStatsContainer.appendChild(statElement);
            });
        }
    }

    // 종족 변경
    async changeCharacterRace() {
        const confirmed = confirm('정말로 종족을 변경하시겠습니까?\n기존 캐릭터가 삭제되고 새로운 캐릭터가 생성됩니다.');
        if (!confirmed) return;
        
        try {
            this.hideMyCharacterModal();
            await this.showCharacterCreationModal();
        } catch (error) {
            console.error('종족 변경 오류:', error);
            alert('종족 변경 중 오류가 발생했습니다.');
        }
    }

    // === 유틸리티 메서드 ===

    // 엘리먼트 업데이트 유틸리티
    updateElement(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = value;
        } else {
            console.warn(`${elementId} 엘리먼트를 찾을 수 없습니다.`);
        }
    }

    // 모든 모달 숨기기
    hideAllModals() {
        this.hideCharacterCreationModal();
        this.hideMyCharacterModal();
    }

    // 전역 함수들을 클래스 메서드로 바인딩 (HTML에서 호출용)
    bindGlobalFunctions() {
        // 캐릭터 생성 관련
        window.showCharacterCreationModal = () => this.showCharacterCreationModal();
        window.hideCharacterCreationModal = () => this.hideCharacterCreationModal();
        window.createCharacter = () => this.createCharacter();
        
        // 내 캐릭터 관련
        window.showMyCharacterModal = () => this.showMyCharacterModal();
        window.hideMyCharacterModal = () => this.hideMyCharacterModal();
        window.changeCharacterRace = () => this.changeCharacterRace();
    }
}