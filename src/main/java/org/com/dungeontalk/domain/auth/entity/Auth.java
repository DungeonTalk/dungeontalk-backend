package org.com.dungeontalk.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.global.common.entity.BaseEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Auth extends BaseEntity {

//    @EmbeddedId
//    private AuthId authId;
    // ManyToOne으로 member 참조 (member_id 필드와 연동)
    //@MapsId("memberId") // AuthId.memberId와 매핑

    //@JoinColumn(name = "member_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")  // insertable/updatable 옵션 제거
    private Member member;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "token_type")
    private String tokenType;

    // 실제 토큰 길이가 짧으면 저장 못합니다. 안전하게 text로 둡시다.
    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

//    @CreationTimestamp
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
}
