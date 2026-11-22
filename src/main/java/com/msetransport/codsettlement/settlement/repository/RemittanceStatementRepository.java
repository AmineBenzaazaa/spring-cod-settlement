package com.msetransport.codsettlement.settlement.repository;

import com.msetransport.codsettlement.settlement.domain.RemittanceStatement;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemittanceStatementRepository
        extends JpaRepository<RemittanceStatement, Long>, JpaSpecificationExecutor<RemittanceStatement> {

    Optional<RemittanceStatement> findByMerchantIdAndFileSha256(Long merchantId, String fileSha256);

    /** Row lock so a duplicate processing run waits instead of racing the first one. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RemittanceStatement s where s.id = :id")
    Optional<RemittanceStatement> findByIdForUpdate(@Param("id") Long id);
}
