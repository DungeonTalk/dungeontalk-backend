package org.com.dungeontalk.domain.worldtype.repository;

import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 세계관 정보 Repository
 */
@Repository
public interface WorldTypeRepository extends JpaRepository<WorldType, Long> {

    /**
     * 코드로 세계관 조회
     */
    Optional<WorldType> findByCode(String code);

    /**
     * 활성화된 세계관만 조회 (정렬 순서대로)
     */
    @Query("SELECT w FROM WorldType w WHERE w.isActive = true ORDER BY w.sortOrder ASC, w.id ASC")
    List<WorldType> findActiveWorldTypesOrderBySortOrder();

    /**
     * 모든 세계관 조회 (정렬 순서대로)
     */
    @Query("SELECT w FROM WorldType w ORDER BY w.sortOrder ASC, w.id ASC")
    List<WorldType> findAllOrderBySortOrder();

    /**
     * 코드 중복 확인
     */
    boolean existsByCode(String code);

    /**
     * 특정 ID를 제외하고 코드 중복 확인
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}