package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, String> {
    List<Terminal> findByMerchantId(String merchantId);
}
