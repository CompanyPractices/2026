package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AcquirerFeeRepository extends JpaRepository<AcquirerFee, BigDecimal> {
    AcquirerFee findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
            String transmissionDateTime, String stan, String terminalId, BigDecimal amount, String pan);
}
