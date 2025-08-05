package org.com.dungeontalk.global.security;

import lombok.Getter;
import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {


    private final String Id;
    private final String name;
    private final String nickname;


    public CustomUserDetails(Member member) {
        this.Id = member.getId();
        this.name = member.getName();
        this.nickname = member.getNickName();

    }

    // role이 없지만 우선 오류 방지를 위해
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 권한 없음
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return nickname; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
