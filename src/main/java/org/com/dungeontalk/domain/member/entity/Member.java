package org.com.dungeontalk.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.com.dungeontalk.global.common.entity.BaseEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

//    @Id
//    @Column(name = "id", nullable = false)
//    private String id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", length = 20, unique = true)
    private String name;

    @Column(name = "nick_name",  unique = true)
    private String nickName;

//    @CreationTimestamp
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
}
