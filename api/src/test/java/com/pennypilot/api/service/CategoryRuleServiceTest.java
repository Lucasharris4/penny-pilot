package com.pennypilot.api.service;

import com.pennypilot.api.dto.category.CategoryRuleResponse;
import com.pennypilot.api.dto.category.CreateCategoryRuleRequest;
import com.pennypilot.api.dto.category.UpdateCategoryRuleRequest;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.CategoryRule;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.CategoryRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryRuleServiceTest {

    private CategoryRuleRepository ruleRepository;
    private CategoryRepository categoryRepository;
    private CategoryRuleService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(CategoryRuleRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        service = new CategoryRuleService(ruleRepository, categoryRepository);
    }

    // --- list ---

    @Test
    void listRules_returnsRulesWithCategoryNames() {
        CategoryRule rule = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);
        Category category = makeCategory(5L, USER_ID, "Coffee");
        when(ruleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of(rule));
        when(categoryRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(category));

        List<CategoryRuleResponse> result = service.listRules(USER_ID);

        assertEquals(1, result.size());
        assertEquals("STARBUCKS*", result.get(0).matchPattern());
        assertEquals("Coffee", result.get(0).categoryName());
        assertEquals(10, result.get(0).priority());
    }

    @Test
    void listRules_emptyList() {
        when(ruleRepository.findByUserIdOrderByPriorityDesc(USER_ID)).thenReturn(List.of());

        assertTrue(service.listRules(USER_ID).isEmpty());
    }

    // --- create ---

    @Test
    void createRule_success() {
        Category category = makeCategory(5L, USER_ID, "Coffee");
        when(categoryRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(category));
        when(ruleRepository.save(any(CategoryRule.class))).thenAnswer(inv -> {
            CategoryRule r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CategoryRuleResponse response = service.createRule(USER_ID,
                new CreateCategoryRuleRequest("STARBUCKS*", 5L, 10));

        assertEquals(1L, response.id());
        assertEquals("STARBUCKS*", response.matchPattern());
        assertEquals(5L, response.categoryId());
        assertEquals("Coffee", response.categoryName());
        assertEquals(10, response.priority());
    }

    @Test
    void createRule_categoryNotFound_throws() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryRuleService.CategoryNotFoundException.class,
                () -> service.createRule(USER_ID,
                        new CreateCategoryRuleRequest("STARBUCKS*", 99L, 10)));
    }

    // --- update ---

    @Test
    void updateRule_success() {
        CategoryRule existing = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);
        Category newCategory = makeCategory(6L, USER_ID, "Dining");
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(6L, USER_ID)).thenReturn(Optional.of(newCategory));
        when(ruleRepository.save(any(CategoryRule.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRuleResponse response = service.updateRule(USER_ID, 1L,
                new UpdateCategoryRuleRequest("DUNKIN*", 6L, 5));

        assertEquals("DUNKIN*", response.matchPattern());
        assertEquals(6L, response.categoryId());
        assertEquals("Dining", response.categoryName());
        assertEquals(5, response.priority());
    }

    @Test
    void updateRule_notFound_throws() {
        when(ruleRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryRuleService.RuleNotFoundException.class,
                () -> service.updateRule(USER_ID, 99L,
                        new UpdateCategoryRuleRequest("DUNKIN*", 5L, 5)));
    }

    @Test
    void updateRule_categoryNotFound_throws() {
        CategoryRule existing = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryRuleService.CategoryNotFoundException.class,
                () -> service.updateRule(USER_ID, 1L,
                        new UpdateCategoryRuleRequest("DUNKIN*", 99L, 5)));
    }

    // --- delete ---

    @Test
    void deleteRule_success() {
        CategoryRule existing = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));

        service.deleteRule(USER_ID, 1L);

        verify(ruleRepository).delete(existing);
    }

    @Test
    void deleteRule_notFound_throws() {
        when(ruleRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryRuleService.RuleNotFoundException.class,
                () -> service.deleteRule(USER_ID, 99L));
    }

    // --- findMatchingCategoryId ---

    @Test
    void findMatchingCategoryId_matchesHighestPriority() {
        CategoryRule lowPriority = makeRule(1L, USER_ID, "*FOOD*", 10L, 1);
        CategoryRule highPriority = makeRule(2L, USER_ID, "UBER EATS*", 20L, 10);
        // List is pre-sorted by priority desc
        List<CategoryRule> rules = List.of(highPriority, lowPriority);

        Optional<Long> result = service.findMatchingCategoryId(rules, "UBER EATS ORDER #123");

        assertTrue(result.isPresent());
        assertEquals(20L, result.get());
    }

    @Test
    void findMatchingCategoryId_noMatch() {
        CategoryRule rule = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);

        Optional<Long> result = service.findMatchingCategoryId(List.of(rule), "WALMART SUPERCENTER");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMatchingCategoryId_multipleRulesSameCategory() {
        CategoryRule r1 = makeRule(1L, USER_ID, "STARBUCKS*", 5L, 10);
        CategoryRule r2 = makeRule(2L, USER_ID, "DUNKIN*", 5L, 10);

        Optional<Long> result = service.findMatchingCategoryId(List.of(r1, r2), "DUNKIN DONUTS #456");

        assertTrue(result.isPresent());
        assertEquals(5L, result.get());
    }

    // --- helpers ---

    private CategoryRule makeRule(Long id, Long userId, String pattern, Long categoryId, int priority) {
        CategoryRule rule = new CategoryRule();
        rule.setId(id);
        rule.setUserId(userId);
        rule.setMatchPattern(pattern);
        rule.setCategoryId(categoryId);
        rule.setPriority(priority);
        return rule;
    }

    private Category makeCategory(Long id, Long userId, String name) {
        Category c = new Category();
        c.setId(id);
        c.setUserId(userId);
        c.setName(name);
        return c;
    }
}
