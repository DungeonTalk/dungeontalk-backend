// AI 게임 메시지 MongoDB 인덱스 설정 (최적화됨)
// 실행 방법: mongo < ai-game-message-indexes.js

// 데이터베이스 선택 (실제 DB명으로 변경 필요)
use dungeontalk;

// 1. 게임방별 최근 메시지 조회 (AI 컨텍스트용)
// findByAiGameRoomIdOrderByCreatedAtDesc
db.ai_game_messages.createIndex(
    { "aiGameRoomId": 1, "createdAt": -1 },
    { name: "idx_room_created_desc", background: true }
);

// 2. 턴별 메시지 조회 (턴제 게임의 핵심)
// findTurnMessages, findRecentTurnsMessages, findMaxMessageOrderByTurn
db.ai_game_messages.createIndex(
    { "aiGameRoomId": 1, "turnNumber": 1, "messageOrder": 1 },
    { name: "idx_room_turn_order", background: true }
);

// 인덱스 생성 결과 확인
print("=== AI Game Message 핵심 인덱스 생성 완료 ===");
print("총 2개 인덱스 (기본 _id 제외)");
db.ai_game_messages.getIndexes().forEach(function(index) {
    print("인덱스명: " + index.name + " | 키: " + JSON.stringify(index.key));
});

print("\n=== 성능 테스트 ===");
print("✅ 게임방별 최근 메시지: aiGameRoomId + createdAt desc");
print("✅ 턴별 메시지: aiGameRoomId + turnNumber + messageOrder");