package com.pennypilot.api.service;

import com.pennypilot.api.dto.CategoryRuleResponse;
import com.pennypilot.api.dto.CreateCategoryRuleRequest;
import com.pennypilot.api.dto.UpdateCategoryRuleRequest;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.CategoryRule;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.CategoryRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class CategoryRuleService {

    private final CategoryRuleRepository categoryRuleRepository;
    private final CategoryRepository categoryRepository;

    public CategoryRuleService(CategoryRuleRepository categoryRuleRepository,
                               CategoryRepository categoryRepository) {
        this.categoryRuleRepository = categoryRuleRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryRuleResponse> listRules(Long userId) {
        return categoryRuleRepository.findByUserIdOrderByPriorityDesc(userId).stream()
                .map(rule -> {
                    String categoryName = categoryRepository.findByIdAndUserId(rule.getCategoryId(), userId)
                            .map(Category::getName)
                            .orElse(null);
                    return CategoryRuleResponse.from(rule, categoryName);
                })
                .toList();
    }

    public CategoryRuleResponse createRule(Long userId, CreateCategoryRuleRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        CategoryRule rule = new CategoryRule();
        rule.setUserId(userId);
        rule.setMatchPattern(request.matchPattern());
        rule.setCategoryId(request.categoryId());
        rule.setPriority(request.priority());

        CategoryRule saved = categoryRuleRepository.save(rule);
        return CategoryRuleResponse.from(saved, category.getName());
    }

    public CategoryRuleResponse updateRule(Long userId, Long ruleId, UpdateCategoryRuleRequest request) {
        CategoryRule rule = categoryRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));

        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        rule.setMatchPattern(request.matchPattern());
        rule.setCategoryId(request.categoryId());
        rule.setPriority(request.priority());

        CategoryRule saved = categoryRuleRepository.save(rule);
        return CategoryRuleResponse.from(saved, category.getName());
    }

    public void deleteRule(Long userId, Long ruleId) {
        CategoryRule rule = categoryRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));

        categoryRuleRepository.delete(rule);
    }

    /**
     * Matches a text against a glob pattern (case-insensitive).
     * Glob syntax: * matches any sequence of characters.
     */
    public static boolean matchesGlob(String pattern, String text) {
        String regex = "(?i)" + Pattern.quote("")
                + globToRegex(pattern);
        return Pattern.matches(regex, text);
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    /**
     * Given a list of rules (sorted by priority descending) and a merchant/description text,
     * returns the category ID of the highest-priority matching rule, or empty if none match.
     */
    public Optional<Long> findMatchingCategoryId(List<CategoryRule> rules, String text) {
        return rules.stream()
                .filter(rule -> matchesGlob(rule.getMatchPattern(), text))
                .findFirst()
                .map(CategoryRule::getCategoryId);
    }

    public static class RuleNotFoundException extends RuntimeException {
        public RuleNotFoundException(Long id) {
            super("Category rule not found: " + id);
        }
    }

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(Long id) {
            super("Category not found: " + id);
        }
    }
}
