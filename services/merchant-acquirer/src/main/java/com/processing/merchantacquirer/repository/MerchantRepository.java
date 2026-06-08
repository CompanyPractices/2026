package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.Merchant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {
  List<Merchant> findByMccIn(Collection<String> mccCodes);
}
