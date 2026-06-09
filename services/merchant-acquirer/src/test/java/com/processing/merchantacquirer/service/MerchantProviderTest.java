package com.processing.merchantacquirer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.repository.MerchantRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MerchantProviderTest {
  @Mock private MerchantRepository merchantRepository;
  private MerchantProvider merchantProvider;

  private final Scenario scenario =
      new Scenario(List.of("5411", "5499"), 100, 3000, "08:00", "23:00", 99);

  @BeforeEach
  void setUp() {
    merchantProvider = new MerchantProvider(merchantRepository);
  }

  @Test
  void throwExceptionWhenNoMerchantsFound() {
    Collection<String> mccCodes = List.of("0000");
    Scenario scenario = mock(Scenario.class);

    when(merchantRepository.findByMccIn(mccCodes)).thenReturn(Collections.emptyList());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              merchantProvider.getMerchant(mccCodes, scenario);
            });

    assertTrue(exception.getMessage().contains("Merchants with given mcc ([0000]) not found"));

    verify(merchantRepository, times(1)).findByMccIn(mccCodes);
  }

  @Test
  void fallbackToScenarioMccWhenMccCodesIsNull() {
    Collection<String> mccCodes = null;
    List<String> scenarioMcc = List.of("7997");

    Scenario scenario = mock(Scenario.class);
    when(scenario.getMcc()).thenReturn(scenarioMcc);

    Merchant merchant = new Merchant();
    List<Merchant> expectedMerchants = List.of(merchant);

    when(merchantRepository.findByMccIn(scenarioMcc)).thenReturn(expectedMerchants);

    List<Merchant> actualMerchants = merchantProvider.getMerchant(mccCodes, scenario);

    assertNotNull(actualMerchants);
    assertEquals(1, actualMerchants.size());
    verify(scenario, times(1)).getMcc();
    verify(merchantRepository, times(1)).findByMccIn(scenarioMcc);
  }

  @Test
  void returnMerchantsWhenMccCodesProvidedAndFound() {
    Collection<String> mccCodes = List.of("5411", "5812");
    Scenario scenario = mock(Scenario.class);

    Merchant merchant1 = new Merchant();
    Merchant merchant2 = new Merchant();
    List<Merchant> expectedMerchants = List.of(merchant1, merchant2);

    when(merchantRepository.findByMccIn(mccCodes)).thenReturn(expectedMerchants);

    List<Merchant> actualMerchants = merchantProvider.getMerchant(mccCodes, scenario);

    assertNotNull(actualMerchants);
    assertEquals(2, actualMerchants.size());
    assertSame(expectedMerchants, actualMerchants);

    verify(merchantRepository, times(1)).findByMccIn(mccCodes);
    verifyNoInteractions(scenario);
  }
}
