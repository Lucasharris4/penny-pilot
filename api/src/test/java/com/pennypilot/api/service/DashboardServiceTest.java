package com.pennypilot.api.service;

import com.pennypilot.api.dto.dashboard.AvailableMonthsResponse;
import com.pennypilot.api.dto.dashboard.CategoryBreakdown;
import com.pennypilot.api.dto.dashboard.DashboardSummaryResponse;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private DashboardService dashboardService;

    private static final Long USER_ID = 1L;
    private static final String START = "2026-03-01";
    private static final String END = "2026-03-31";

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        dashboardService = new DashboardService(transactionRepository, categoryRepository);
    }

    private Category makeCategory(Long id, String name, String color) {
        Category c = new Category();
        c.setId(id);
        c.setUserId(USER_ID);
        c.setName(name);
        c.setColor(color);
        return c;
    }

    @Test
    void getSummary_returnsIncomeExpenseAndNet() {
        when(transactionRepository.sumByTransactionType(USER_ID, START, END)).thenReturn(Arrays.<Object[]>asList(
                new Object[]{TransactionType.CREDIT, 500000L},
                new Object[]{TransactionType.DEBIT, 300000L}
        ));
        when(transactionRepository.sumByCategoryAndType(USER_ID, START, END, TransactionType.DEBIT))
                .thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary(USER_ID, START, END);

        assertEquals(500000L, result.incomeCents());
        assertEquals(300000L, result.expensesCents());
        assertEquals(200000L, result.netCents());
    }

    @Test
    void getSummary_categoryBreakdownWithPercentages() {
        when(transactionRepository.sumByTransactionType(USER_ID, START, END)).thenReturn(Arrays.<Object[]>asList(
                new Object[]{TransactionType.DEBIT, 100000L}
        ));
        when(transactionRepository.sumByCategoryAndType(USER_ID, START, END, TransactionType.DEBIT))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[]{10L, 60000L},
                        new Object[]{20L, 40000L}
                ));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(
                makeCategory(10L, "Groceries", "#4CAF50"),
                makeCategory(20L, "Dining", "#FF9800")
        ));

        DashboardSummaryResponse result = dashboardService.getSummary(USER_ID, START, END);

        assertEquals(2, result.byCategory().size());
        CategoryBreakdown groceries = result.byCategory().get(0);
        assertEquals("Groceries", groceries.categoryName());
        assertEquals(60000L, groceries.amountCents());
        assertEquals(60.0, groceries.percentage());

        CategoryBreakdown dining = result.byCategory().get(1);
        assertEquals("Dining", dining.categoryName());
        assertEquals(40000L, dining.amountCents());
        assertEquals(40.0, dining.percentage());
    }

    @Test
    void getSummary_uncategorizedTransactionsShowAsOther() {
        when(transactionRepository.sumByTransactionType(USER_ID, START, END)).thenReturn(Arrays.<Object[]>asList(
                new Object[]{TransactionType.DEBIT, 50000L}
        ));
        when(transactionRepository.sumByCategoryAndType(USER_ID, START, END, TransactionType.DEBIT))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[]{null, 50000L}
                ));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary(USER_ID, START, END);

        assertEquals(1, result.byCategory().size());
        assertEquals("Other", result.byCategory().get(0).categoryName());
        assertEquals("#9E9E9E", result.byCategory().get(0).categoryColor());
    }

    @Test
    void getSummary_noTransactions_returnsZeros() {
        when(transactionRepository.sumByTransactionType(USER_ID, START, END)).thenReturn(List.of());
        when(transactionRepository.sumByCategoryAndType(USER_ID, START, END, TransactionType.DEBIT))
                .thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary(USER_ID, START, END);

        assertEquals(0L, result.incomeCents());
        assertEquals(0L, result.expensesCents());
        assertEquals(0L, result.netCents());
        assertTrue(result.byCategory().isEmpty());
    }

    @Test
    void getSummary_sortsByCategoryAmountDescending() {
        when(transactionRepository.sumByTransactionType(USER_ID, START, END)).thenReturn(Arrays.<Object[]>asList(
                new Object[]{TransactionType.DEBIT, 100000L}
        ));
        when(transactionRepository.sumByCategoryAndType(USER_ID, START, END, TransactionType.DEBIT))
                .thenReturn(Arrays.<Object[]>asList(
                        new Object[]{10L, 20000L},
                        new Object[]{20L, 80000L}
                ));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(
                makeCategory(10L, "Small", "#111"),
                makeCategory(20L, "Big", "#222")
        ));

        DashboardSummaryResponse result = dashboardService.getSummary(USER_ID, START, END);

        assertEquals("Big", result.byCategory().get(0).categoryName());
        assertEquals("Small", result.byCategory().get(1).categoryName());
    }

    @Test
    void getAvailableMonths_returnsDistinctMonths() {
        when(transactionRepository.findDistinctMonthsByUserId(USER_ID))
                .thenReturn(List.of("2026-04", "2026-03", "2026-02"));

        AvailableMonthsResponse result = dashboardService.getAvailableMonths(USER_ID);

        assertEquals(List.of("2026-04", "2026-03", "2026-02"), result.months());
    }

    @Test
    void getAvailableMonths_noTransactions_returnsEmpty() {
        when(transactionRepository.findDistinctMonthsByUserId(USER_ID)).thenReturn(List.of());

        AvailableMonthsResponse result = dashboardService.getAvailableMonths(USER_ID);

        assertTrue(result.months().isEmpty());
    }
}
