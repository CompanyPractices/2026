package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.models.BinIssuer;
import com.processing.cardmanagement.repositories.BinIssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

@RequiredArgsConstructor
public class BinIssuerInitializer implements ApplicationRunner {

    private final BinIssuerRepository repository;

    private final List<BinIssuer> BINS_ISSUERS = List.of(
            new BinIssuer("400000", "ISS001"),
            new BinIssuer("400001", "ISS002"),
            new BinIssuer("400002", "ISS003"),
            new BinIssuer("400003", "ISS004"),
            new BinIssuer("400004", "ISS005")
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (BinIssuer binIssuer : BINS_ISSUERS) {
            if (!repository.existsByBin(binIssuer.bin())) {
                repository.save(binIssuer);
            }
        }
    }
}
