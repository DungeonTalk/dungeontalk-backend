package org.com.dungeontalk.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.com.dungeontalk.domain.member.entity.Member;

public record RegisterRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(max = 30, message = "아이디는 최대 30자까지 가능합니다.")
        String name,

        @Size(max = 30, message = "닉네임은 최대 30자까지 가능합니다.")
        String nickName,

        @NotBlank(message = "비밀번호는 필수입니다.")
        // @Size(min = 2, max = 128, message = "비밀번호는 최소 2자 이상이어야 합니다.")
        String password
) {
    public Member toEntity(String id,  String encodedPassword) {
        return Member.builder()
                .id(id)
                .name(this.name)
                .nickName(this.nickName)
                .password(this.password)
                .password(encodedPassword)
                .build();
    }
}
