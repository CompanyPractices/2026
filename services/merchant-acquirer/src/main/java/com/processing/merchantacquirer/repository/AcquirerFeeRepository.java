package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.AcquirerFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcquirerFeeRepository extends JpaRepository<AcquirerFee, Long> {
    AcquirerFee findByTransmissionDateTimeAndStanAndTerminalIdAndAmountAndPan(
            String transmissionDateTime, String stan, String terminalId, Long amount, String pan);
}
