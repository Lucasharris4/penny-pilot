package com.pennypilot.api.dto.category;

import com.pennypilot.api.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category response")
public record CategoryResponse(
        @Schema(description = "Category ID")
        Long id,

        @Schema(description = "Category name")
        String name,

        @Schema(description = "Icon (emoji or icon name)")
        String icon,

        @Schema(description = "Hex color for charts")
        String color,

        @Schema(description = "Whether this is a subscription-type category")
        boolean isSubscription
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.isSubscription()
        );
    }
}
