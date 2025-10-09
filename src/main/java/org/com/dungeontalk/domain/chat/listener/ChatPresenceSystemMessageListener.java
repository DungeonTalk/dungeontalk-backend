package org.com.dungeontalk.domain.chat.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.com.dungeontalk.domain.chat.event.ChatPresenceEvent;
import org.com.dungeontalk.domain.chat.repository.ChatMessageRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.RedisPublisher;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPresenceSystemMessageListener {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 트랜잭션 커밋 이후에만 실행 → DB 일관성 보장
     * @TransactionalEventListener(AFTER_COMMIT) 덕분에,
     * Mongo 기록이 커밋된 뒤에만 메시지가 저장/브로드캐스트돼서 불일치가 생기지 않음.
     *
     * @Async 추가로 비동기 처리 → 트랜잭션 커밋을 블로킹하지 않음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("chatEventExecutor")
    public void onPresence(ChatPresenceEvent ev) {
        try {
            String nickName = memberRepository.findById(ev.getMemberId())
                .map(Member::getNickName)
                .orElse("알 수 없음");

            String content = (ev.getType() == MessageType.JOIN)
                ? nickName + "이 입장했습니다."
                : nickName + "이 퇴장했습니다.";

            ChatMessage msg = ChatMessage.builder()
                .messageId(UuidV7Creator.create())
                .roomId(ev.getRoomId())
                .senderId(ev.getMemberId())
                .content(content)
                .type(ev.getType())
                .createdAt(ev.getOccurredAt())
                .updatedAt(ev.getOccurredAt())
                .build();

            ChatMessage saved = chatMessageRepository.save(msg);
            ChatMessageDto dto = ChatMessageDto.fromEntity(saved, nickName);

            // Redis Pub/Sub (문자열로 발행: RedisSubscriber가 JSON 파싱) - 비동기 처리
            redisPublisher.publishAsync(ev.getRoomId(), objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            log.warn("시스템 메시지 발행 실패(roomId={}, memberId={}, type={}): {}",
                ev.getRoomId(), ev.getMemberId(), ev.getType(), e.getMessage());
        }
    }

}
