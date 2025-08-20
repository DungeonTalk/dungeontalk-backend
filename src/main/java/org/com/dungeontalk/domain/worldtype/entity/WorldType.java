package org.com.dungeontalk.domain.worldtype.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 게임 세계관 정보를 저장하는 JPA 엔티티
 */
@Entity
@Table(name = "world_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WorldType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 기존 enum name (FANTASY, SF, MODERN)

    @Column(nullable = false, length = 100)
    private String displayName; // 화면에 표시될 이름

    @Column(nullable = false, length = 500)
    private String description; // 세계관 설명

    @Column(nullable = false, length = 1000)
    private String gameSettings; // AI 게임에서 사용할 설정 문구

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 활성화 여부

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0; // 정렬 순서

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Redis 큐 키 생성
     */
    public String getQueueKey() {
        return "matching:queue:" + this.code;
    }

    /**
     * Redis 통계 키 생성
     */
    public String getStatsKey() {
        return "matching:stats:" + this.code;
    }

    /**
     * 세계관 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 세계관 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 세계관 정보 업데이트
     */
    public void update(String displayName, String description, String gameSettings, Integer sortOrder) {
        this.displayName = displayName;
        this.description = description;
        this.gameSettings = gameSettings;
        this.sortOrder = sortOrder;
    }
}