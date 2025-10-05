package org.com.dungeontalk.domain.chat.repository;

import java.util.Optional;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    // 정원만 빠르게 읽기(필드 제한) — Optional 사용
    @Query(value = "{ '_id': ?0 }", fields = "{ '_id': 1, 'maxCapacity': 1 }")
    Optional<ChatRoomCapacityView> findCapacityById(String roomId);

    interface ChatRoomCapacityView {
        String getId();
        Integer getMaxCapacity();
    }

}
