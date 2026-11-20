package com.benzaazaa.codsettlement.logistics.repository;

import com.benzaazaa.codsettlement.logistics.domain.Courier;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourierRepository extends JpaRepository<Courier, Long>, JpaSpecificationExecutor<Courier> {

    Optional<Courier> findByIdAndActiveTrue(Long id);

    Optional<Courier> findByCode(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCode(String code);
}
