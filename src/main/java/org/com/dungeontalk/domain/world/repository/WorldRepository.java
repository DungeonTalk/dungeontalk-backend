package org.com.dungeontalk.domain.world.repository;

import org.com.dungeontalk.domain.world.entity.World;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldRepository extends JpaRepository<World, Long> {
}