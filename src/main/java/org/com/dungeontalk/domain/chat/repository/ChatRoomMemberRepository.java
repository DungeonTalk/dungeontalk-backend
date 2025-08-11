package org.com.dungeontalk.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomMemberRepository extends MongoRepository<ChatRoomMember, String> {
    List<ChatRoomMember> findByRoomId(String roomId);

    Optional<ChatRoomMember> findByRoomIdAndMemberId(String roomId, String memberId);

    boolean existsByRoomIdAndMemberId(String roomId, String memberId);

    long countByRoomIdAndStatus(String roomId, Status status);

    List<ChatRoomMember> findByRoomIdAndStatus(String roomId, Status status);   // ONLINE 목록

    long deleteByRoomId(String roomId);                                         // 방 제거 시 정리
}
