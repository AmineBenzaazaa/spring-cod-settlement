package com.benzaazaa.codsettlement.account.repository;

import com.benzaazaa.codsettlement.account.domain.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findBySlug(String slug);
}
