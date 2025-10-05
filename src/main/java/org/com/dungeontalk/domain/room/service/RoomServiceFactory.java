package org.com.dungeontalk.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 룸 서비스 팩토리
 * 룸 타입에 따라 적절한 RoomService 구현체를 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomServiceFactory {

    private final List<RoomService> roomServices;
    private Map<RoomType, RoomService> serviceMap;

    /**
     * 팩토리 초기화
     * 등록된 모든 RoomService 구현체를 맵에 저장
     */
    private void initializeServiceMap() {
        if (serviceMap == null) {
            serviceMap = roomServices.stream()
                    .collect(Collectors.toMap(
                            RoomService::getSupportedRoomType,
                            Function.identity(),
                            (existing, replacement) -> {
                                log.warn("중복된 RoomType 발견: {}. 기존: {}, 새로운: {}", 
                                        existing.getSupportedRoomType(), 
                                        existing.getClass().getSimpleName(), 
                                        replacement.getClass().getSimpleName());
                                return replacement; // 나중에 등록된 것으로 대체
                            }
                    ));
            
            log.info("RoomServiceFactory 초기화 완료. 등록된 서비스: {}", 
                    serviceMap.keySet().stream()
                            .map(type -> type.getDisplayName())
                            .collect(Collectors.joining(", ")));
        }
    }

    /**
     * 룸 타입에 해당하는 서비스를 반환합니다
     * 
     * @param roomType 룸 타입
     * @return 해당 타입의 RoomService 구현체
     * @throws IllegalArgumentException 지원하지 않는 룸 타입인 경우
     */
    public RoomService getService(RoomType roomType) {
        if (roomType == null) {
            throw new IllegalArgumentException("룸 타입이 null입니다");
        }

        initializeServiceMap();

        RoomService service = serviceMap.get(roomType);
        if (service == null) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 룸 타입입니다: %s. 지원 가능한 타입: %s", 
                            roomType.getDisplayName(),
                            serviceMap.keySet().stream()
                                    .map(RoomType::getDisplayName)
                                    .collect(Collectors.joining(", "))
                    )
            );
        }

        log.debug("RoomService 반환: {} -> {}", 
                roomType.getDisplayName(), 
                service.getClass().getSimpleName());
        
        return service;
    }

    /**
     * 룸 타입 코드로 서비스를 반환합니다
     * 
     * @param roomTypeCode 룸 타입 코드 ("ai", "chat")
     * @return 해당 타입의 RoomService 구현체
     */
    public RoomService getService(String roomTypeCode) {
        RoomType roomType = RoomType.fromCode(roomTypeCode);
        return getService(roomType);
    }

    /**
     * 등록된 모든 룸 서비스 목록을 반환합니다
     * 
     * @return 등록된 RoomService 목록
     */
    public List<RoomService> getAllServices() {
        return List.copyOf(roomServices);
    }

    /**
     * 지원하는 모든 룸 타입을 반환합니다
     * 
     * @return 지원하는 RoomType 목록
     */
    public List<RoomType> getSupportedRoomTypes() {
        initializeServiceMap();
        return List.copyOf(serviceMap.keySet());
    }

    /**
     * 특정 룸 타입이 지원되는지 확인합니다
     * 
     * @param roomType 확인할 룸 타입
     * @return 지원 여부
     */
    public boolean isSupported(RoomType roomType) {
        initializeServiceMap();
        return serviceMap.containsKey(roomType);
    }

    /**
     * 특정 룸 타입 코드가 지원되는지 확인합니다
     * 
     * @param roomTypeCode 확인할 룸 타입 코드
     * @return 지원 여부
     */
    public boolean isSupported(String roomTypeCode) {
        try {
            RoomType roomType = RoomType.fromCode(roomTypeCode);
            return isSupported(roomType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 팩토리 상태 정보를 반환합니다 (디버깅/모니터링 용)
     * 
     * @return 팩토리 상태 정보
     */
    public String getFactoryStatus() {
        initializeServiceMap();
        
        StringBuilder status = new StringBuilder();
        status.append("RoomServiceFactory Status:\n");
        status.append("- 등록된 서비스 수: ").append(serviceMap.size()).append("\n");
        
        serviceMap.forEach((type, service) -> {
            status.append("- ").append(type.getDisplayName())
                  .append(" (").append(type.getCode()).append(")")
                  .append(" -> ").append(service.getClass().getSimpleName())
                  .append("\n");
        });
        
        return status.toString();
    }
}