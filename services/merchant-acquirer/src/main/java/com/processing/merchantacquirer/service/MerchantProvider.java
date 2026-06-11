package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.repository.MerchantRepository;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
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
  public Long getMerchantAcquirerFee(String id) {
    Merchant merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Merchant not found with id: " + id));
    return merchant.getAcquiringFee();
  }

  public List<Merchant> getAll() {
    return merchantRepository.findAll();
  }

  public long count() {
    return merchantRepository.count();
  }
}
