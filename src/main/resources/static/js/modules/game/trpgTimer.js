// TRPG 15분 타이머 시스템
export class TRPGTimer {
    constructor() {
        this.gameStartTime = null;
        this.timerInterval = null;
        this.targetGameDuration = 15; // 15분
        this.currentGamePhase = 'waiting';
        this.gamePhasesData = {
            waiting: { name: '준비 중...', pressure: 'relaxed' },
            intro: { name: '🌅 도입', pressure: 'relaxed' },
            development: { name: '⚡ 전개', pressure: 'normal' },
            middle: { name: '🔥 중반', pressure: 'normal' },
            climax: { name: '💥 클라이맥스', pressure: 'urgent' },
            ending: { name: '🎭 종료', pressure: 'critical' }
        };
        this.onTimeEndCallback = null;
        this.onPhaseChangeCallback = null;
    }

    // TRPG 게임 타이머 시작
    startTRPGTimer() {
        if (this.gameStartTime) return; // 이미 시작됨
        
        this.gameStartTime = Date.now();
        window.gameStartTime = this.gameStartTime; // 전역 변수로 설정
        console.log('🕐 TRPG 15분 타이머 시작!');
        
        // 1초마다 타이머 업데이트
        this.timerInterval = setInterval(() => this.updateTRPGTimer(), 1000);
        
        // 첫 업데이트
        this.updateTRPGTimer();
    }

    // 타이머 업데이트
    updateTRPGTimer() {
        if (!this.gameStartTime) return;
        
        const elapsed = (Date.now() - this.gameStartTime) / 1000; // 초 단위
        const elapsedMinutes = elapsed / 60;
        const remaining = Math.max(0, (this.targetGameDuration * 60) - elapsed);
        const remainingMinutes = Math.floor(remaining / 60);
        const remainingSeconds = Math.floor(remaining % 60);
        
        // UI 업데이트
        this.updateTimerDisplay(remainingMinutes, remainingSeconds, remaining);
        
        // 게임 단계 계산 및 업데이트
        this.updateGamePhase(elapsedMinutes, remaining);
        
        // 시간 종료 체크
        if (remaining <= 0) {
            this.handleGameTimeEnd();
        }
    }

    // 타이머 디스플레이 업데이트
    updateTimerDisplay(remainingMinutes, remainingSeconds, remaining) {
        // 타이머 텍스트 업데이트
        const timerText = document.querySelector('.timer-text');
        if (timerText) {
            timerText.textContent = `${remainingMinutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
        }
        
        // 프로그레스 바 업데이트
        const progress = document.querySelector('.timer-progress');
        if (progress) {
            const percentage = Math.max(0, (remaining / (this.targetGameDuration * 60)) * 100);
            progress.style.width = `${percentage}%`;
        }
    }

    // 게임 단계 업데이트
    updateGamePhase(elapsedMinutes, remaining) {
        let newPhase = this.currentGamePhase;
        
        if (remaining <= 0) {
            newPhase = 'ending';
        } else if (remaining <= 3 * 60) { // 3분 이하
            newPhase = 'climax';
        } else if (remaining <= 7 * 60) { // 7분 이하
            newPhase = 'middle';
        } else if (elapsedMinutes >= 2) { // 2분 경과
            newPhase = 'development';
        } else if (elapsedMinutes > 0) {
            newPhase = 'intro';
        }
        
        if (newPhase !== this.currentGamePhase) {
            const oldPhase = this.currentGamePhase;
            this.currentGamePhase = newPhase;
            this.updatePhaseUI();
            console.log(`🎭 게임 단계 변경: ${this.gamePhasesData[this.currentGamePhase].name}`);
            
            // 콜백 실행
            if (this.onPhaseChangeCallback) {
                this.onPhaseChangeCallback(newPhase, oldPhase);
            }
        }
    }

    // 게임 단계 UI 업데이트
    updatePhaseUI() {
        const phaseData = this.gamePhasesData[this.currentGamePhase];
        
        // 단계 텍스트 업데이트
        const phaseText = document.querySelector('.phase-text');
        if (phaseText) {
            phaseText.textContent = phaseData.name;
        }
        
        // 압박도 텍스트 및 스타일 업데이트
        const pressureText = document.querySelector('.pressure-text');
        if (pressureText) {
            const pressureNames = {
                relaxed: '여유',
                normal: '보통',
                urgent: '급박',
                critical: '매우 급박'
            };
            
            pressureText.textContent = pressureNames[phaseData.pressure];
            pressureText.className = `pressure-text pressure-${phaseData.pressure}`;
        }
    }

    // 게임 시간 종료 처리
    handleGameTimeEnd() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
        
        console.log('⏰ TRPG 15분 시간 종료!');
        
        // UI 업데이트
        const timerText = document.querySelector('.timer-text');
        if (timerText) {
            timerText.textContent = '00:00';
            timerText.style.color = '#F44336';
        }
        
        // 콜백 실행
        if (this.onTimeEndCallback) {
            this.onTimeEndCallback();
        }
    }

    // 타이머 초기화
    resetTRPGTimer() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
        
        this.gameStartTime = null;
        window.gameStartTime = null;
        this.currentGamePhase = 'waiting';
        
        const timerText = document.querySelector('.timer-text');
        if (timerText) {
            timerText.textContent = '15:00';
            timerText.style.color = '#333';
        }
        
        const progress = document.querySelector('.timer-progress');
        if (progress) {
            progress.style.width = '100%';
        }
        
        this.updatePhaseUI();
        console.log('🔄 TRPG 타이머 초기화');
    }

    // 타이머 일시정지
    pauseTimer() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
            console.log('⏸️ TRPG 타이머 일시정지');
        }
    }

    // 타이머 재시작
    resumeTimer() {
        if (!this.timerInterval && this.gameStartTime) {
            this.timerInterval = setInterval(() => this.updateTRPGTimer(), 1000);
            console.log('▶️ TRPG 타이머 재시작');
        }
    }

    // 콜백 설정 메서드
    onTimeEnd(callback) {
        this.onTimeEndCallback = callback;
    }

    onPhaseChange(callback) {
        this.onPhaseChangeCallback = callback;
    }

    // getter methods
    getGameStartTime() {
        return this.gameStartTime;
    }

    getCurrentPhase() {
        return this.currentGamePhase;
    }

    getRemainingTime() {
        if (!this.gameStartTime) return this.targetGameDuration * 60;
        
        const elapsed = (Date.now() - this.gameStartTime) / 1000;
        return Math.max(0, (this.targetGameDuration * 60) - elapsed);
    }

    getRemainingMinutes() {
        return Math.floor(this.getRemainingTime() / 60);
    }

    getRemainingSeconds() {
        return Math.floor(this.getRemainingTime() % 60);
    }

    getElapsedTime() {
        if (!this.gameStartTime) return 0;
        return (Date.now() - this.gameStartTime) / 1000;
    }

    isRunning() {
        return this.timerInterval !== null;
    }

    isTimeUp() {
        return this.getRemainingTime() <= 0;
    }

    // 타이머 정보를 문자열로 반환
    getTimerDisplayString() {
        const minutes = this.getRemainingMinutes();
        const seconds = this.getRemainingSeconds();
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }

    // 게임 단계 정보 반환
    getCurrentPhaseInfo() {
        return this.gamePhasesData[this.currentGamePhase];
    }
}