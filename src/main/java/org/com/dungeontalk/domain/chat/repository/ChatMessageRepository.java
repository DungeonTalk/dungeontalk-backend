package org.com.dungeontalk.domain.chat.repository;

import java.util.List;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    Page<ChatMessage> findByRoomId(String roomId, Pageable pageable);
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

//    Slice<ChatMessage> findByRoomId(String roomId, Pageable pageable);              // 무한스크롤 최적 (total count x)

    // 초기 로드/리프레시: 최근 50건
    List<ChatMessage> findTop50ByRoomIdOrderByCreatedAtDesc(String roomId);         // 초기 로드용

    long deleteByRoomId(String roomId); // 방 삭제 시 정리


}
