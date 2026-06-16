package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AcquirerFeeRepository extends JpaRepository<AcquirerFee, Long> {
    AcquirerFee findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
            Instant transmissionDateTime, String stan, String terminalId, Long amount, String pan);
}
