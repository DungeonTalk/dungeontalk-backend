package org.com.dungeontalk.domain.auth.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthId implements Serializable {
    private String id;
    private String memberId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthId)) return false;
        AuthId authId = (AuthId) o;
        return Objects.equals(id, authId.id) &&
                Objects.equals(memberId, authId.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, memberId);
    }
}
