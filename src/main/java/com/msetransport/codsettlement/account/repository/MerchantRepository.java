package com.msetransport.codsettlement.account.repository;

import com.msetransport.codsettlement.account.domain.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findBySlug(String slug);
}
