package com.processing.merchantacquirer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.repository.MerchantRepository;

import java.math.BigDecimal;
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
      new Scenario(List.of("5411", "5499"), BigDecimal.valueOf(100), BigDecimal.valueOf(3000), "08:00", "23:00", 99);

  @BeforeEach
  void setUp() {
    merchantProvider = new MerchantProvider(merchantRepository);
  }

  @Test
  void throwExceptionWhenNoMerchantsFound() {
    List<String> mccCodes = List.of("0000");

    when(merchantRepository.findByMccIn(mccCodes)).thenReturn(Collections.emptyList());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> merchantProvider.getMerchant(mccCodes, scenario));

    assertEquals("Merchants with given mcc ([0000]) not found", exception.getMessage());

    verify(merchantRepository, times(1)).findByMccIn(mccCodes);
  }

  @Test
  void fallbackToScenarioMccWhenMccCodesIsNull() {
    List<String> mccCodes = null;
    List<String> scenarioMcc = scenario.getMcc();

    Merchant merchant = new Merchant(
            "MERCH00000000007",
            "Ашан Сити",
            "5411",
            "grocery",
            "ACQ003",
            BigDecimal.valueOf(15),
            145000);
    List<Merchant> expectedMerchants = List.of(merchant);

    when(merchantRepository.findByMccIn(scenarioMcc)).thenReturn(expectedMerchants);

    List<Merchant> actualMerchants = merchantProvider.getMerchant(mccCodes, scenario);

    assertEquals(expectedMerchants, actualMerchants);
    assertEquals(1, actualMerchants.size());
    verify(merchantRepository, times(1)).findByMccIn(scenarioMcc);
  }

  @Test
  void returnMerchantsWhenMccCodesProvidedAndFound() {
    List<String> mccCodes = List.of("5411", "5499");

    Merchant merchant1 = new Merchant(
            "MERCH00000000007",
            "Ашан Сити",
            "5411",
            "grocery",
            "ACQ003",
            BigDecimal.valueOf(15),
            145000);
    Merchant merchant2 = new Merchant(
            "MERCH00000000007",
            "ВБ Сити",
            "5499",
            "grocery",
            "ACQ003",
            BigDecimal.valueOf(15),
            145000);
    List<Merchant> expectedMerchants = List.of(merchant1, merchant2);

    when(merchantRepository.findByMccIn(mccCodes)).thenReturn(expectedMerchants);

    List<Merchant> actualMerchants = merchantProvider.getMerchant(mccCodes, scenario);

    assertEquals(2, actualMerchants.size());
    assertEquals(expectedMerchants, actualMerchants);

    verify(merchantRepository, times(1)).findByMccIn(mccCodes);
  }

  @Test
  void getAllMerchants(){
    Merchant merchant1 = new Merchant(
            "MERCH00000000007",
            "Ашан Сити",
            "5411",
            "grocery",
            "ACQ003",
            BigDecimal.valueOf(15),
            145000);
    Merchant merchant2 = new Merchant(
            "MERCH00000000007",
            "ВБ Сити",
            "5499",
            "grocery",
            "ACQ003",
            BigDecimal.valueOf(15),
            145000);
    List<Merchant> expectedMerchants = List.of(merchant1, merchant2);

    when(merchantRepository.findAll()).thenReturn(List.of(merchant1, merchant2));

    List<Merchant> result = merchantProvider.getAll();

    assertEquals(2, result.size());
    assertEquals(expectedMerchants, result);
    verify(merchantRepository, times(1)).findAll();
  }

  @Test
  void getCountsOfMerchants() {
    when(merchantRepository.count()).thenReturn(42L);

    long result = merchantProvider.count();

    assertEquals(42L, result);
    verify(merchantRepository, times(1)).count();
  }
}
