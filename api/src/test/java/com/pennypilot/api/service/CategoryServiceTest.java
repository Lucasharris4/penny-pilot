package com.pennypilot.api.service;

import com.pennypilot.api.config.CategoryProperties;
import com.pennypilot.api.dto.CategoryResponse;
import com.pennypilot.api.dto.CreateCategoryRequest;
import com.pennypilot.api.dto.UpdateCategoryRequest;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private CategoryService categoryService;

    private static final Long USER_ID = 1L;
    private static final List<CategoryProperties.DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new CategoryProperties.DefaultCategory("Groceries", "\uD83D\uDED2", "#4CAF50", false),
            new CategoryProperties.DefaultCategory("Subscriptions", "\uD83D\uDD04", "#607D8B", true)
    );

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        CategoryProperties props = new CategoryProperties(true, DEFAULT_CATEGORIES);
        categoryService = new CategoryService(categoryRepository, props);
    }

    // --- list ---

    @Test
    void listCategories_returnsUserCategories() {
        Category c1 = makeCategory(1L, USER_ID, "Groceries", "\uD83D\uDED2", "#4CAF50", false);
        Category c2 = makeCategory(2L, USER_ID, "Dining", "\uD83C\uDF7D\uFE0F", "#FF9800", false);
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(c1, c2));

        List<CategoryResponse> result = categoryService.listCategories(USER_ID);

        assertEquals(2, result.size());
        assertEquals("Groceries", result.get(0).name());
        assertEquals("Dining", result.get(1).name());
    }

    @Test
    void listCategories_emptyList() {
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.listCategories(USER_ID);

        assertTrue(result.isEmpty());
    }

    // --- create ---

    @Test
    void createCategory_success() {
        when(categoryRepository.existsByNameAndUserId("Coffee", USER_ID)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(USER_ID,
                new CreateCategoryRequest("Coffee", "\u2615", "#8B4513", false));

        assertEquals(1L, response.id());
        assertEquals("Coffee", response.name());
        assertEquals("\u2615", response.icon());
        assertEquals("#8B4513", response.color());
        assertFalse(response.isSubscription());
    }

    @Test
    void createCategory_subscriptionFlag() {
        when(categoryRepository.existsByNameAndUserId("Netflix", USER_ID)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(USER_ID,
                new CreateCategoryRequest("Netflix", "\uD83D\uDD04", "#607D8B", true));

        assertTrue(response.isSubscription());
    }

    @Test
    void createCategory_nullOptionalFields() {
        when(categoryRepository.existsByNameAndUserId("Other", USER_ID)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(USER_ID,
                new CreateCategoryRequest("Other", null, null, null));

        assertEquals("Other", response.name());
        assertNull(response.icon());
        assertNull(response.color());
        assertFalse(response.isSubscription());
    }

    @Test
    void createCategory_duplicateName_throws() {
        when(categoryRepository.existsByNameAndUserId("Groceries", USER_ID)).thenReturn(true);

        assertThrows(CategoryService.CategoryNameExistsException.class,
                () -> categoryService.createCategory(USER_ID,
                        new CreateCategoryRequest("Groceries", null, null, null)));
    }

    // --- update ---

    @Test
    void updateCategory_success() {
        Category existing = makeCategory(1L, USER_ID, "Grocries", "\uD83D\uDED2", "#4CAF50", false);
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndUserId("Groceries", USER_ID)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(USER_ID, 1L,
                new UpdateCategoryRequest("Groceries", "\uD83D\uDED2", "#4CAF50", false));

        assertEquals("Groceries", response.name());
    }

    @Test
    void updateCategory_sameNameAllowed() {
        Category existing = makeCategory(1L, USER_ID, "Groceries", "\uD83D\uDED2", "#4CAF50", false);
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(USER_ID, 1L,
                new UpdateCategoryRequest("Groceries", "\uD83C\uDF4E", "#FF0000", false));

        assertEquals("Groceries", response.name());
        assertEquals("\uD83C\uDF4E", response.icon());
    }

    @Test
    void updateCategory_nameConflict_throws() {
        Category existing = makeCategory(1L, USER_ID, "Coffee", null, null, false);
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndUserId("Groceries", USER_ID)).thenReturn(true);

        assertThrows(CategoryService.CategoryNameExistsException.class,
                () -> categoryService.updateCategory(USER_ID, 1L,
                        new UpdateCategoryRequest("Groceries", null, null, false)));
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryService.CategoryNotFoundException.class,
                () -> categoryService.updateCategory(USER_ID, 99L,
                        new UpdateCategoryRequest("Groceries", null, null, false)));
    }

    // --- delete ---

    @Test
    void deleteCategory_success() {
        Category existing = makeCategory(1L, USER_ID, "Coffee", null, null, false);
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));

        categoryService.deleteCategory(USER_ID, 1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryService.CategoryNotFoundException.class,
                () -> categoryService.deleteCategory(USER_ID, 99L));
    }

    @Test
    void deleteCategory_otherUsersCategory_throws() {
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(CategoryService.CategoryNotFoundException.class,
                () -> categoryService.deleteCategory(USER_ID, 1L));
    }

    // --- seedDefaults ---

    @Test
    @SuppressWarnings("unchecked")
    void seedDefaults_createsConfiguredCategories() {
        categoryService.seedDefaults(USER_ID);

        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<Category> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("Groceries", saved.get(0).getName());
        assertEquals(USER_ID, saved.get(0).getUserId());
        assertFalse(saved.get(0).isSubscription());
        assertEquals("Subscriptions", saved.get(1).getName());
        assertTrue(saved.get(1).isSubscription());
    }

    @Test
    void seedDefaults_disabled_doesNothing() {
        CategoryProperties disabledProps = new CategoryProperties(false, DEFAULT_CATEGORIES);
        CategoryService disabledService = new CategoryService(categoryRepository, disabledProps);

        disabledService.seedDefaults(USER_ID);

        verify(categoryRepository, never()).saveAll(any());
    }

    @Test
    void seedDefaults_nullDefaults_doesNothing() {
        CategoryProperties nullProps = new CategoryProperties(true, null);
        CategoryService nullService = new CategoryService(categoryRepository, nullProps);

        nullService.seedDefaults(USER_ID);

        verify(categoryRepository, never()).saveAll(any());
    }

    // --- helpers ---

    private Category makeCategory(Long id, Long userId, String name, String icon, String color, boolean isSubscription) {
        Category c = new Category();
        c.setId(id);
        c.setUserId(userId);
        c.setName(name);
        c.setIcon(icon);
        c.setColor(color);
        c.setSubscription(isSubscription);
        return c;
    }
}
