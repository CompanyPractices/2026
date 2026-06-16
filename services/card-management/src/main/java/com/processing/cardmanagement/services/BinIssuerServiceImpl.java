package com.processing.cardmanagement.services;

import com.processing.cardmanagement.exceptions.BinAlreadyExistException;
import com.processing.cardmanagement.exceptions.BinNotFoundException;
import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.models.BinIssuerEntity;
import com.processing.cardmanagement.repositories.BinIssuerJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BinIssuerServiceImpl implements BinIssuerService {

    private final BinIssuerJpaRepository repository;

    @Override
    public String getIssuerId(String bin) {
        return repository.findById(bin)
                .map(BinIssuerEntity::getIssuerId)
                .orElseThrow(() -> new BinNotFoundException("Unknown BIN: " + bin));
    }

    @Override
    public List<BinIssuer> getAll() {
        return repository.findAll()
                .stream()
                .map(e -> new BinIssuer(e.getBin(), e.getIssuerId()))
                .toList();
    }

    @Override
    public BinIssuer create(String bin, String issuerId) {
        if (repository.existsById(bin)) {
            throw new BinAlreadyExistException(bin);
        }
        BinIssuerEntity saved = repository.save(new BinIssuerEntity(bin, issuerId));
        return new BinIssuer(saved.getBin(), saved.getIssuerId());
    }

    @Override
    public void delete(String bin) {
        if (!repository.existsById(bin)) {
            throw new BinNotFoundException(bin);
        }
        repository.deleteById(bin);
    }
}
