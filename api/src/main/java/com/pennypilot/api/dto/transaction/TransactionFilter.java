package com.pennypilot.api.dto.transaction;

import com.pennypilot.api.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Transaction filter criteria")
public record TransactionFilter(
        @Schema(description = "Start date (inclusive, ISO 8601)")
        String startDate,

        @Schema(description = "End date (inclusive, ISO 8601)")
        String endDate,

        @Schema(description = "Filter by category ID")
        Long categoryId,

        @Schema(description = "Minimum amount in cents")
        Integer minAmount,

        @Schema(description = "Maximum amount in cents")
        Integer maxAmount,

        @Schema(description = "Search description or merchant name")
        String search
) {
    public Specification<Transaction> toSpecification(Long userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amountCents"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amountCents"), maxAmount));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate merchantMatch = cb.like(cb.lower(root.get("merchantName")), pattern);
                predicates.add(cb.or(descMatch, merchantMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
