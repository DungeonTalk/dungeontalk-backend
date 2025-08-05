package org.com.dungeontalk.domain.auth.repository;

import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.entity.AuthId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<Auth, AuthId> {
}
