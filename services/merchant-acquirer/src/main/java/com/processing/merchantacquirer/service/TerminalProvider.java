package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class TerminalProvider {
    private final TerminalRepository terminalRepository;

    private final Map<String, List<Terminal>> terminalsByMerchants;

    public Terminal getByMerchant(String merchantId) {
        if (!terminalsByMerchants.containsKey(merchantId)) {
            terminalsByMerchants.put(merchantId, terminalRepository.findByMerchantId(merchantId));
        }

        List<Terminal> terminals = terminalsByMerchants.get(merchantId);
        if (terminals.isEmpty()) {
            throw new NullPointerException("Merchant not have terminals, merchant id: " + merchantId);
        }

        return terminals.get(ThreadLocalRandom.current().nextInt(0, terminals.size()));
    }
}
