package com.pennypilot.api.repository;

import com.pennypilot.api.entity.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {

    List<CategoryRule> findByUserIdOrderByPriorityDesc(Long userId);

    Optional<CategoryRule> findByIdAndUserId(Long id, Long userId);
}
