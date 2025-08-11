package org.com.dungeontalk.global.security;

import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    //private final Member member;

//    public JwtAuthenticationToken(Member member) {
//        //super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))); // 권한 설정
//        super(Collections.emptyList()); // 아직은 권한 없음
//        this.member = member;
//        setAuthenticated(false);
//    }

    private final CustomUserDetails userDetails;

    public JwtAuthenticationToken(CustomUserDetails userDetails) {
        super(Collections.emptyList()); // 권한 없으면 emptyList
        this.userDetails = userDetails;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }

//    @Override
//    public Object getPrincipal() {
//        return member;
//    }
}
