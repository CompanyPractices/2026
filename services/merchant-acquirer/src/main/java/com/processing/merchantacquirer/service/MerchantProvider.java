package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.repository.MerchantRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantProvider {
  private final MerchantRepository merchantRepository;

  public List<Merchant> getMerchant(Collection<String> mccCodes, Scenario scenario) {
    mccCodes = mccCodes == null ? scenario.getMcc() : mccCodes;
    List<Merchant> merchants = merchantRepository.findByMccIn(mccCodes);

    if (merchants.isEmpty()) {
      throw new IllegalArgumentException("Merchants with given mcc (" + mccCodes + ") not found");
    }

    return merchants;
  }

  public List<Merchant> getAll() {
    return merchantRepository.findAll();
  }

  public Long count() {
    return merchantRepository.count();
  }
}
