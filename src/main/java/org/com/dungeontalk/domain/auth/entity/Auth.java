package org.com.dungeontalk.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.com.dungeontalk.domain.member.entity.Member;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth {

    @EmbeddedId
    private AuthId authId;

    // ManyToOne으로 member 참조 (member_id 필드와 연동)
    @MapsId("memberId") // AuthId.memberId와 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "token_type", length = 20)
    private String tokenType;

    // 실제 토큰 길이가 짧으면 저장 못합니다. 안전하게 text로 둡시다.
    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
