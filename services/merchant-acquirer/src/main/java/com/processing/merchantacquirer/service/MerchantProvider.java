package com.processing.merchantacquirer.service;

import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Scenario;
import com.processing.merchantacquirer.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MerchantProvider {
    private final MerchantRepository merchantRepository;

    public List<Merchant> getMerchant(Collection<String> mccCodes, Scenario scenario){
        mccCodes = mccCodes == null ? scenario.getMcc() : mccCodes;

        return merchantRepository.findByMccIn(mccCodes);
    }
}
