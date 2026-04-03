package com.pennypilot.api.service;

import com.pennypilot.api.dto.transaction.*;
import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<TransactionResponse> listTransactions(Long userId, TransactionFilter filter, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findAll(filter.toSpecification(userId), pageable);

        Map<Long, String> categoryNames = loadCategoryNames(userId);

        return page.map(txn -> TransactionResponse.from(txn, resolveCategoryName(txn.getCategoryId(), categoryNames)));
    }

    public TransactionResponse updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (request.categoryId() != null) {
            categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        }

        transaction.setCategoryId(request.categoryId());
        transaction.setAmountCents(request.amountCents());
        transaction.setTransactionType(request.transactionType());
        transaction.setDescription(request.description());
        transaction.setMerchantName(request.merchantName());
        transaction.setDate(request.date());

        Transaction saved = transactionRepository.save(transaction);

        Map<Long, String> categoryNames = loadCategoryNames(userId);
        return TransactionResponse.from(saved, resolveCategoryName(saved.getCategoryId(), categoryNames));
    }

    @Transactional
    public BulkCategorizeResponse bulkCategorize(Long userId, BulkCategorizeRequest request) {
        categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));

        List<Transaction> transactions = transactionRepository.findAllByIdInAndUserId(request.transactionIds(), userId);

        List<Long> foundIds = transactions.stream().map(Transaction::getId).toList();
        List<Long> invalidIds = request.transactionIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new InvalidTransactionIdsException(invalidIds);
        }

        transactions.forEach(txn -> txn.setCategoryId(request.categoryId()));
        transactionRepository.saveAll(transactions);

        return new BulkCategorizeResponse(transactions.size());
    }

    private Map<Long, String> loadCategoryNames(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        com.pennypilot.api.entity.Category::getId,
                        com.pennypilot.api.entity.Category::getName));
    }

    private String resolveCategoryName(Long categoryId, Map<Long, String> categoryNames) {
        if (categoryId == null) {
            return "Other";
        }
        return categoryNames.getOrDefault(categoryId, "Other");
    }

    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(Long id) {
            super("Transaction not found: " + id);
        }
    }

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(Long id) {
            super("Category not found: " + id);
        }
    }

    public static class InvalidTransactionIdsException extends RuntimeException {
        private final List<Long> invalidIds;

        public InvalidTransactionIdsException(List<Long> invalidIds) {
            super("Invalid transaction IDs: " + invalidIds);
            this.invalidIds = invalidIds;
        }

        public List<Long> getInvalidIds() {
            return invalidIds;
        }
    }
}
