package com.pennypilot.api.service;

import com.pennypilot.api.dto.transaction.BulkCategorizeRequest;
import com.pennypilot.api.dto.transaction.BulkCategorizeResponse;
import com.pennypilot.api.dto.transaction.TransactionFilter;
import com.pennypilot.api.dto.transaction.TransactionResponse;
import com.pennypilot.api.dto.transaction.UpdateTransactionRequest;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.entity.TransactionType;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private TransactionService transactionService;

    private static final Long USER_ID = 1L;
    private static final TransactionFilter EMPTY_FILTER = new TransactionFilter(null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        transactionService = new TransactionService(transactionRepository, categoryRepository);
    }

    // --- list ---

    @Test
    @SuppressWarnings("unchecked")
    void listTransactions_returnsMappedPage() {
        Transaction txn = makeTransaction(1L, USER_ID, 1L, 5L, 4500, TransactionType.DEBIT,
                "WHOLE FOODS #1234", "Whole Foods", "2026-03-15");
        Category cat = makeCategory(5L, USER_ID, "Groceries", "🛒", "#4CAF50");

        Page<Transaction> page = new PageImpl<>(List.of(txn), PageRequest.of(0, 20), 1);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));

        Page<TransactionResponse> result = transactionService.listTransactions(
                USER_ID, EMPTY_FILTER, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(1L, response.id());
        assertEquals("Groceries", response.categoryName());
        assertEquals(4500, response.amountCents());
        assertEquals(TransactionType.DEBIT, response.transactionType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listTransactions_uncategorized_showsOther() {
        Transaction txn = makeTransaction(1L, USER_ID, 1L, null, 1000, TransactionType.DEBIT,
                "UNKNOWN MERCHANT", null, "2026-03-15");

        Page<Transaction> page = new PageImpl<>(List.of(txn), PageRequest.of(0, 20), 1);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        Page<TransactionResponse> result = transactionService.listTransactions(
                USER_ID, EMPTY_FILTER, PageRequest.of(0, 20));

        assertEquals("Other", result.getContent().get(0).categoryName());
        assertNull(result.getContent().get(0).categoryId());
    }

    // --- update ---

    @Test
    void updateTransaction_updatesAllFields() {
        Transaction existing = makeTransaction(1L, USER_ID, 1L, 5L, 4500, TransactionType.DEBIT,
                "OLD DESC", "Old Merchant", "2026-03-15");
        Category cat = makeCategory(6L, USER_ID, "Dining", "🍽️", "#FF9800");

        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(6L, USER_ID)).thenReturn(Optional.of(cat));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(cat));

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                6L, 5000, TransactionType.CREDIT, "NEW DESC", "New Merchant", "2026-03-20");

        TransactionResponse result = transactionService.updateTransaction(USER_ID, 1L, request);

        assertEquals(6L, result.categoryId());
        assertEquals("Dining", result.categoryName());
        assertEquals(5000, result.amountCents());
        assertEquals(TransactionType.CREDIT, result.transactionType());
        assertEquals("NEW DESC", result.description());
        assertEquals("New Merchant", result.merchantName());
        assertEquals("2026-03-20", result.date());
    }

    @Test
    void updateTransaction_nullCategory_uncategorizes() {
        Transaction existing = makeTransaction(1L, USER_ID, 1L, 5L, 4500, TransactionType.DEBIT,
                "DESC", "Merchant", "2026-03-15");

        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of());

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, 4500, TransactionType.DEBIT, "DESC", "Merchant", "2026-03-15");

        TransactionResponse result = transactionService.updateTransaction(USER_ID, 1L, request);

        assertNull(result.categoryId());
        assertEquals("Other", result.categoryName());
    }

    @Test
    void updateTransaction_notFound_throwsException() {
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null, 4500, TransactionType.DEBIT, "DESC", "Merchant", "2026-03-15");

        assertThrows(TransactionService.TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(USER_ID, 99L, request));
    }

    @Test
    void updateTransaction_invalidCategory_throwsException() {
        Transaction existing = makeTransaction(1L, USER_ID, 1L, null, 4500, TransactionType.DEBIT,
                "DESC", "Merchant", "2026-03-15");

        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                99L, 4500, TransactionType.DEBIT, "DESC", "Merchant", "2026-03-15");

        assertThrows(TransactionService.CategoryNotFoundException.class,
                () -> transactionService.updateTransaction(USER_ID, 1L, request));
    }

    // --- bulk categorize ---

    @Test
    void bulkCategorize_updatesAllTransactions() {
        Category cat = makeCategory(5L, USER_ID, "Groceries", "🛒", "#4CAF50");
        Transaction t1 = makeTransaction(1L, USER_ID, 1L, null, 1000, TransactionType.DEBIT, "D1", "M1", "2026-03-15");
        Transaction t2 = makeTransaction(2L, USER_ID, 1L, null, 2000, TransactionType.DEBIT, "D2", "M2", "2026-03-16");

        when(categoryRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(cat));
        when(transactionRepository.findAllByIdInAndUserId(List.of(1L, 2L), USER_ID)).thenReturn(List.of(t1, t2));

        BulkCategorizeRequest request = new BulkCategorizeRequest(List.of(1L, 2L), 5L);
        BulkCategorizeResponse result = transactionService.bulkCategorize(USER_ID, request);

        assertEquals(2, result.updated());
        assertEquals(5L, t1.getCategoryId());
        assertEquals(5L, t2.getCategoryId());
        verify(transactionRepository).saveAll(List.of(t1, t2));
    }

    @Test
    void bulkCategorize_invalidIds_throwsException() {
        Category cat = makeCategory(5L, USER_ID, "Groceries", "🛒", "#4CAF50");
        Transaction t1 = makeTransaction(1L, USER_ID, 1L, null, 1000, TransactionType.DEBIT, "D1", "M1", "2026-03-15");

        when(categoryRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(cat));
        when(transactionRepository.findAllByIdInAndUserId(List.of(1L, 99L), USER_ID)).thenReturn(List.of(t1));

        BulkCategorizeRequest request = new BulkCategorizeRequest(List.of(1L, 99L), 5L);

        TransactionService.InvalidTransactionIdsException ex = assertThrows(
                TransactionService.InvalidTransactionIdsException.class,
                () -> transactionService.bulkCategorize(USER_ID, request));

        assertEquals(List.of(99L), ex.getInvalidIds());
    }

    @Test
    void bulkCategorize_invalidCategory_throwsException() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        BulkCategorizeRequest request = new BulkCategorizeRequest(List.of(1L), 99L);

        assertThrows(TransactionService.CategoryNotFoundException.class,
                () -> transactionService.bulkCategorize(USER_ID, request));
    }

    // --- helpers ---

    private Transaction makeTransaction(Long id, Long userId, Long accountId, Long categoryId,
                                         int amountCents, TransactionType type, String description,
                                         String merchantName, String date) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setUserId(userId);
        t.setAccountId(accountId);
        t.setCategoryId(categoryId);
        t.setAmountCents(amountCents);
        t.setTransactionType(type);
        t.setDescription(description);
        t.setMerchantName(merchantName);
        t.setDate(date);
        return t;
    }

    private Category makeCategory(Long id, Long userId, String name, String icon, String color) {
        Category c = new Category();
        c.setId(id);
        c.setUserId(userId);
        c.setName(name);
        c.setIcon(icon);
        c.setColor(color);
        return c;
    }
}
