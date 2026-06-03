package com.processing.service;

import com.processing.dto.TransactionSearchResponse;
import com.processing.model.Transaction;
import com.processing.repository.TransactionRepository;
import com.processing.specification.TransactionFilter;
import com.processing.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionSearchResponse search(TransactionFilter filter) {
        Pageable pageable = PageRequest.of(filter.getOffset(), filter.getLimit());
        Page<Transaction> page = transactionRepository.findAll(TransactionSpecification.filter(filter), pageable);
        return new TransactionSearchResponse(page.getTotalElements(), page.getContent());

    }
}
