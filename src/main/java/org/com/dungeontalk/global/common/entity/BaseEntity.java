package org.com.dungeontalk.global.common.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BaseEntity {

    /* BaseEntity를 상속받는 모든 모든 엔티티는 UUID7 과 생성 시각과 수정 시각을 자동으로 관리 */

    @Id
    private String id;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;

    // UUID 자동 할당
    @PrePersist
    public void generateIdIfNull() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UuidV7Creator.create();
        }
    }

}