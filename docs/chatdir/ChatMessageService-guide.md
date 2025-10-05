
### ChatMessageService 리팩토링 설명

- 정원/접속자 반영: joinRoom/leaveRoom에 위임(내부에서 DB maxCapacity + Redis 인원 체크 & presence 브로드캐스트).
- idempotent: 실제 변화가 있을 때만 시스템 메시지 저장, 아니면 NOOP DTO 반환(JOIN_IGNORED/LEAVE_IGNORED).
- 가독성: saveSystemMessage, saveTalkMessage, buildNoopMessage, publishToRoom, resolveNickname로 역할 분리.
- 기존 기능 유지: 페이지 조회/닉네임 매핑/Redis Pub/Sub 경로 그대로.