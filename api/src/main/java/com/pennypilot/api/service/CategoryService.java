package com.pennypilot.api.service;

import com.pennypilot.api.dto.CategoryResponse;
import com.pennypilot.api.dto.CreateCategoryRequest;
import com.pennypilot.api.dto.UpdateCategoryRequest;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listCategories(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        if (categoryRepository.existsByNameAndUserId(request.name(), userId)) {
            throw new CategoryNameExistsException(request.name());
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.name());
        category.setIcon(request.icon());
        category.setColor(request.color());
        category.setSubscription(request.isSubscription() != null && request.isSubscription());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public CategoryResponse updateCategory(Long userId, Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByNameAndUserId(request.name(), userId)) {
            throw new CategoryNameExistsException(request.name());
        }

        category.setName(request.name());
        category.setIcon(request.icon());
        category.setColor(request.color());
        category.setSubscription(request.isSubscription() != null && request.isSubscription());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        categoryRepository.delete(category);
    }

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(Long id) {
            super("Category not found: " + id);
        }
    }

    public static class CategoryNameExistsException extends RuntimeException {
        public CategoryNameExistsException(String name) {
            super("Category already exists: " + name);
        }
    }
}
