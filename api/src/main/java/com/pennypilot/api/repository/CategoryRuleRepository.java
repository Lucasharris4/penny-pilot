package com.pennypilot.api.repository;

import com.pennypilot.api.entity.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {

    List<CategoryRule> findByUserIdOrderByPriorityDesc(Long userId);

    Optional<CategoryRule> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(MAX(r.priority), 0) FROM CategoryRule r WHERE r.userId = :userId")
    int findMaxPriorityByUserId(@Param("userId") Long userId);
}
