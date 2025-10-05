export default {
    template: `
        <div class="mb-8">
            <div class="glass-card p-6">
                <h3 class="text-xl font-bold mb-4">🎮 게임 상태</h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div class="stat-container">
                        <div class="text-sm text-light-darker">현재 상태</div>
                        <div class="text-lg font-bold">{{ currentStatus }}</div>
                    </div>
                    <div class="stat-container">
                        <div class="text-sm text-light-darker">세계관</div>
                        <div class="text-lg font-bold">{{ selectedWorldName || '선택 안됨' }}</div>
                    </div>
                    <div class="stat-container">
                        <div class="text-sm text-light-darker">매칭 정보</div>
                        <div class="text-lg font-bold">{{ matchingInfo }}</div>
                    </div>
                    <div class="stat-container">
                        <div class="text-sm text-light-darker">게임방</div>
                        <div class="text-lg font-bold">{{ roomInfo }}</div>
                    </div>
                </div>
            </div>
        </div>
    `,
    props: {
        currentStatus: {
            type: String,
            default: '로그인 필요'
        },
        selectedWorldName: {
            type: String,
            default: null
        },
        matchingInfo: {
            type: String,
            default: '대기 중'
        },
        roomInfo: {
            type: String,
            default: '미연결'
        }
    }
};