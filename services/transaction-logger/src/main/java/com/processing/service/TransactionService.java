package com.processing.service;

import com.processing.dto.DashboardStatsResponse;
import com.processing.dto.TransactionSearchResponse;
import com.processing.enums.TransactionStatus;
import com.processing.model.Transaction;
import com.processing.model.Transaction_;
import com.processing.repository.TransactionRepository;
import com.processing.specification.TransactionFilter;
import com.processing.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionSearchResponse search(TransactionFilter filter) {
        Pageable pageable = PageRequest.of(filter.getOffset() / filter.getLimit(), filter.getLimit());
        Page<Transaction> page = transactionRepository.findAll(TransactionSpecification.filter(filter), pageable);
        return new TransactionSearchResponse(page.getTotalElements(), page.getContent());
    }

    public DashboardStatsResponse getStats() {
        long total = transactionRepository.count();
        long approved = transactionRepository.countByStatus(TransactionStatus.APPROVED);
        long declined = transactionRepository.countByStatus(TransactionStatus.DECLINED);
        long totalAmount = total > 0 ? transactionRepository.sumAmount() : 0;
        long recentCount = transactionRepository.countByCreatedAtAfter(Instant.now().minusSeconds(60));
        double avgProcessingTimeMs = total > 0 ? transactionRepository.averageProcessingTimeMs() : 0;
        return new DashboardStatsResponse(
                total,
                approved,
                declined,
                total > 0 ? (double) approved / total : 0,
                totalAmount,
                total > 0 ? totalAmount / total : 0,
                avgProcessingTimeMs,
                recentCount
        );
    }

    public List<Transaction> getRecent(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, Transaction_.CREATED_AT));
        return transactionRepository.findAll(pageable).getContent();
    }
}
