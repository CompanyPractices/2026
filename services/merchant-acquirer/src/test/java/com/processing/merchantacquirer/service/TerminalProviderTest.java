package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.exception.ResourceNotFoundException;
import com.processing.merchantacquirer.repository.TerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TerminalProviderTest {
    private TerminalRepository terminalRepository;
    private TerminalProvider terminalProvider;

    private static final String MERCHANT_ID = "MERCH0000000007";

    @BeforeEach
    void setUp() {
        terminalRepository = Mockito.mock(TerminalRepository.class);
        terminalProvider = new TerminalProvider(terminalRepository);
    }

    @Test
    void returnsTerminalsByMerchant() {
        Terminal terminal = new Terminal("TERM0001", "POS", null);
        when(terminalRepository.findByMerchantId(MERCHANT_ID)).thenReturn(List.of(terminal));

        Terminal result = terminalProvider.getByMerchant(MERCHANT_ID);

        assertEquals("TERM0001", result.getId());
        assertEquals("POS", result.getType());
    }

    @Test
    void callRepositoryOnce() {
        Terminal terminal = new Terminal("TERM0001", "POS", null);
        when(terminalRepository.findByMerchantId(MERCHANT_ID)).thenReturn(List.of(terminal));

        terminalProvider.getByMerchant(MERCHANT_ID);
        terminalProvider.getByMerchant(MERCHANT_ID);
        terminalProvider.getByMerchant(MERCHANT_ID);

        verify(terminalRepository, times(1)).findByMerchantId(MERCHANT_ID);
    }

    @Test
    void throwsWhenMerchantHasNotReturnedTerminals() {
        when(terminalRepository.findByMerchantId(MERCHANT_ID)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> terminalProvider.getByMerchant(MERCHANT_ID));
    }
}
