package com.benzaazaa.codsettlement.settlement.repository;

import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemittanceLineRepository extends JpaRepository<RemittanceLine, Long> {

    List<RemittanceLine> findByStatementIdOrderByRowNumber(Long statementId);

    /**
     * One {@code DELETE} statement instead of loading and deleting entities one by one.
     * Bulk JPQL bypasses the persistence context, hence flush before and clear after.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RemittanceLine l where l.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") Long statementId);
}
