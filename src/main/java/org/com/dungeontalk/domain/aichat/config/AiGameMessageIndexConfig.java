package org.com.dungeontalk.domain.aichat.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 게임 메시지 MongoDB 인덱스 자동 생성 설정
 * 
 * 애플리케이션 시작 시 필요한 인덱스들을 자동으로 생성합니다.
 * 성능 최적화를 위해 자주 사용되는 쿼리 패턴에 맞는 인덱스를 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiGameMessageIndexConfig {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "ai_game_messages";
    
    @org.springframework.beans.factory.annotation.Value("${app.mongodb.auto-index:true}")
    private boolean autoCreateIndex;

    @PostConstruct
    public void initIndexes() {
        if (!autoCreateIndex) {
            log.info("MongoDB 인덱스 자동 생성이 비활성화되어 있습니다");
            return;
        }
        
        log.info("AI 게임 메시지 MongoDB 인덱스 초기화 시작");
        
        IndexOperations indexOps = mongoTemplate.indexOps(COLLECTION_NAME);
        
        try {
            // MongoDB 연결 테스트 (인증 확인)
            List<IndexInfo> existingIndexes = indexOps.getIndexInfo();
            log.info("기존 인덱스 개수: {}", existingIndexes.size());
            
            // 1. 게임방별 최근 메시지 조회 (AI 컨텍스트용 - 핵심)
            createIndexIfNotExists(indexOps, "idx_room_created_desc",
                new Index().on("aiGameRoomId", Sort.Direction.ASC)
                          .on("createdAt", Sort.Direction.DESC)
                          .background());
            
            // 2. 턴별 메시지 조회 (턴제 게임 핵심)
            createIndexIfNotExists(indexOps, "idx_room_turn_order",
                new Index().on("aiGameRoomId", Sort.Direction.ASC)
                          .on("turnNumber", Sort.Direction.ASC)
                          .on("messageOrder", Sort.Direction.ASC)
                          .background());
            
            log.info("AI 게임 메시지 인덱스 초기화 완료");
            
        } catch (Exception e) {
            // 인증 오류인 경우 로그 레벨 조정
            if (e.getMessage() != null && e.getMessage().contains("authentication")) {
                log.warn("MongoDB 인증이 필요합니다. 인덱스 자동 생성을 건너뜁니다: {}", e.getMessage());
                log.info("해결 방법: application.yml에 MongoDB 인증 정보를 추가하거나 MongoDB를 --noauth 모드로 실행하세요");
            } else {
                log.error("인덱스 생성 중 오류 발생", e);
            }
        }
    }
    
    private void createIndexIfNotExists(IndexOperations indexOps, String indexName, Index index) {
        try {
            // 인덱스 존재 여부 확인
            List<IndexInfo> existingIndexes = indexOps.getIndexInfo();
            boolean indexExists = existingIndexes.stream()
                    .anyMatch(info -> indexName.equals(info.getName()));
            
            if (!indexExists) {
                index.named(indexName);
                indexOps.ensureIndex(index);
                log.info("인덱스 생성 완료: {}", indexName);
            } else {
                log.debug("인덱스 이미 존재: {}", indexName);
            }
        } catch (Exception e) {
            log.warn("인덱스 {} 생성 실패: {}", indexName, e.getMessage());
        }
    }
}