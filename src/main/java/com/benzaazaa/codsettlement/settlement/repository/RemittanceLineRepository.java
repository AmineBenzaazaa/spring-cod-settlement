package com.benzaazaa.codsettlement.settlement.repository;

import com.benzaazaa.codsettlement.settlement.domain.RemittanceLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemittanceLineRepository extends JpaRepository<RemittanceLine, Long> {

    List<RemittanceLine> findByStatementIdOrderByRowNumber(Long statementId);

    @Query("""
            select new com.benzaazaa.codsettlement.settlement.repository.MoneyTotals(
                sum(l.amountCollected), sum(l.fee))
            from RemittanceLine l
            where l.statement.id = :statementId""")
    MoneyTotals totalsForStatement(@Param("statementId") Long statementId);

    /**
     * One {@code DELETE} statement instead of loading and deleting entities one by one.
     * Bulk JPQL bypasses the persistence context: lines are never loaded before this call
     * in the same transaction, so there is nothing stale to clear.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from RemittanceLine l where l.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") Long statementId);
}
