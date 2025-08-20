package org.com.dungeontalk.domain.gamecharacter.repository;

import org.com.dungeontalk.domain.gamecharacter.entity.RequestExp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestExpRepository extends JpaRepository<RequestExp, Integer> {

    Optional<RequestExp> findByLevel(int level);
}