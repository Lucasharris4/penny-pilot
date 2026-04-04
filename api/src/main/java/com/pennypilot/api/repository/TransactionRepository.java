package com.pennypilot.api.repository;

import com.pennypilot.api.entity.Transaction;
import com.pennypilot.api.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findAllByIdInAndUserId(List<Long> ids, Long userId);

    void deleteByAccountIdAndUserId(Long accountId, Long userId);

    List<Transaction> findByAccountIdAndExternalIdIn(Long accountId, java.util.Collection<String> externalIds);

    @Query("SELECT t.transactionType, SUM(t.amountCents) FROM Transaction t " +
            "WHERE t.userId = :userId AND t.date >= :startDate AND t.date <= :endDate " +
            "GROUP BY t.transactionType")
    List<Object[]> sumByTransactionType(@Param("userId") Long userId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate);

    @Query("SELECT t.categoryId, SUM(t.amountCents) FROM Transaction t " +
            "WHERE t.userId = :userId AND t.date >= :startDate AND t.date <= :endDate " +
            "AND t.transactionType = :type " +
            "GROUP BY t.categoryId")
    List<Object[]> sumByCategoryAndType(@Param("userId") Long userId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("type") TransactionType type);

    @Query("SELECT DISTINCT SUBSTRING(t.date, 1, 7) FROM Transaction t " +
            "WHERE t.userId = :userId ORDER BY SUBSTRING(t.date, 1, 7) DESC")
    List<String> findDistinctMonthsByUserId(@Param("userId") Long userId);
}
