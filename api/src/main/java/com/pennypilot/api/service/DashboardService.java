package com.pennypilot.api.service;

import com.pennypilot.api.dto.dashboard.AvailableMonthsResponse;
import com.pennypilot.api.dto.dashboard.CategoryBreakdown;
import com.pennypilot.api.dto.dashboard.DashboardSummaryResponse;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public DashboardService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public DashboardSummaryResponse getSummary(Long userId, String startDate, String endDate) {
        // Get totals by transaction type
        List<Object[]> typeSums = transactionRepository.sumByTransactionType(userId, startDate, endDate);
        long incomeCents = 0;
        long expensesCents = 0;
        for (Object[] row : typeSums) {
            TransactionType type = (TransactionType) row[0];
            long amount = ((Number) row[1]).longValue();
            if (type == TransactionType.CREDIT) {
                incomeCents = amount;
            } else {
                expensesCents = amount;
            }
        }

        // Get expense breakdown by category
        List<Object[]> categorySums = transactionRepository.sumByCategoryAndType(
                userId, startDate, endDate, TransactionType.DEBIT);

        // Load user's categories for name/color lookup
        Map<Long, Category> categoryMap = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<CategoryBreakdown> byCategory = new ArrayList<>();
        for (Object[] row : categorySums) {
            Long categoryId = (Long) row[0];
            long amount = ((Number) row[1]).longValue();
            double percentage = expensesCents > 0 ? (amount * 100.0) / expensesCents : 0;

            String name;
            String color;
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                Category cat = categoryMap.get(categoryId);
                name = cat.getName();
                color = cat.getColor();
            } else {
                name = "Other";
                color = "#9E9E9E";
            }

            byCategory.add(new CategoryBreakdown(categoryId, name, color, amount, Math.round(percentage * 10.0) / 10.0));
        }

        // Sort by amount descending
        byCategory.sort((a, b) -> Long.compare(b.amountCents(), a.amountCents()));

        return new DashboardSummaryResponse(incomeCents, expensesCents, incomeCents - expensesCents, byCategory);
    }

    public AvailableMonthsResponse getAvailableMonths(Long userId) {
        List<String> months = transactionRepository.findDistinctMonthsByUserId(userId);
        return new AvailableMonthsResponse(months);
    }
}
