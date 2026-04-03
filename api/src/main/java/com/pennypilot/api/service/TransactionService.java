package com.pennypilot.api.service;

import com.pennypilot.api.dto.transaction.*;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    public Page<TransactionResponse> listTransactions(Long userId, String startDate, String endDate,
                                                       Long categoryId, Integer minAmount, Integer maxAmount,
                                                       String search, Pageable pageable) {
        Specification<Transaction> spec = buildSpecification(userId, startDate, endDate, categoryId,
                minAmount, maxAmount, search);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

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

    public List<TransactionSummaryResponse> getSummary(Long userId, String startDate, String endDate) {
        Specification<Transaction> spec = buildSpecification(userId, startDate, endDate, null, null, null, null);
        List<Transaction> transactions = transactionRepository.findAll(spec);

        Map<Long, String> categoryNames = loadCategoryNames(userId);
        Map<Long, Category> categoryMap = categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(txn -> txn.getCategoryId() != null ? txn.getCategoryId() : -1L));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long catId = entry.getKey() == -1L ? null : entry.getKey();
                    List<Transaction> txns = entry.getValue();
                    Category category = catId != null ? categoryMap.get(catId) : null;

                    long totalCents = txns.stream()
                            .mapToLong(Transaction::getAmountCents)
                            .sum();

                    return new TransactionSummaryResponse(
                            catId,
                            category != null ? category.getName() : "Other",
                            category != null ? category.getColor() : null,
                            category != null ? category.getIcon() : null,
                            totalCents,
                            txns.size()
                    );
                })
                .toList();
    }

    private Specification<Transaction> buildSpecification(Long userId, String startDate, String endDate,
                                                           Long categoryId, Integer minAmount, Integer maxAmount,
                                                           String search) {
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

    private Map<Long, String> loadCategoryNames(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
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
