package com.pennypilot.api.dto.category;

import com.pennypilot.api.entity.CategoryRule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category rule response")
public record CategoryRuleResponse(
        @Schema(description = "Rule ID")
        Long id,

        @Schema(description = "Glob-style match pattern")
        String matchPattern,

        @Schema(description = "Category ID")
        Long categoryId,

        @Schema(description = "Category name")
        String categoryName,

        @Schema(description = "Priority (higher wins on conflicts)")
        int priority
) {
    public static CategoryRuleResponse from(CategoryRule rule, String categoryName) {
        return new CategoryRuleResponse(
                rule.getId(),
                rule.getMatchPattern(),
                rule.getCategoryId(),
                categoryName,
                rule.getPriority()
        );
    }
}
