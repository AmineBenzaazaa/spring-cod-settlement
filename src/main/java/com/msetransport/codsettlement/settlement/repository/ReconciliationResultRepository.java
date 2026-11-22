package com.msetransport.codsettlement.settlement.repository;

import com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome;
import com.msetransport.codsettlement.settlement.domain.ReconciliationResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {

    /** {@code left join fetch}: results and their (optional) line in a single query. */
    @Query("""
            select r from ReconciliationResult r left join fetch r.line
            where r.statement.id = :statementId
            order by r.outcome, r.trackingNumber""")
    List<ReconciliationResult> findForReport(@Param("statementId") Long statementId);

    @Query("""
            select r from ReconciliationResult r left join fetch r.line
            where r.statement.id = :statementId and r.outcome = :outcome
            order by r.trackingNumber""")
    List<ReconciliationResult> findForReport(@Param("statementId") Long statementId,
                                             @Param("outcome") ReconciliationOutcome outcome);

    @Query("select r from ReconciliationResult r join fetch r.statement where r.id = :id")
    Optional<ReconciliationResult> findWithStatement(@Param("id") Long id);

    @Query("""
            select new com.msetransport.codsettlement.settlement.repository.OutcomeTotals(
                r.outcome, count(r), sum(r.expectedAmount))
            from ReconciliationResult r
            where r.statement.id = :statementId
            group by r.outcome""")
    List<OutcomeTotals> totalsByOutcome(@Param("statementId") Long statementId);

    /**
     * Non-matched results on reconciled statements: how many, and the absolute amount at stake
     * ({@code abs}: a short payment and an over-payment both need attention).
     */
    @Query("""
            select new com.msetransport.codsettlement.settlement.repository.CountAndSum(count(r), sum(abs(r.difference)))
            from ReconciliationResult r
            where r.outcome <> com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome.MATCHED
              and r.statement.status = com.msetransport.codsettlement.settlement.domain.StatementStatus.RECONCILED""")
    CountAndSum discrepancyTotals();

    @Query("""
            select new com.msetransport.codsettlement.settlement.repository.CountAndSum(count(r), sum(abs(r.difference)))
            from ReconciliationResult r
            where r.outcome <> com.msetransport.codsettlement.settlement.domain.ReconciliationOutcome.MATCHED
              and r.statement.status = com.msetransport.codsettlement.settlement.domain.StatementStatus.RECONCILED
              and r.statement.merchant.id = :merchantId""")
    CountAndSum discrepancyTotals(@Param("merchantId") Long merchantId);

    /**
     * Bulk delete. {@code flushAutomatically} pushes pending changes first. We do not use
     * {@code clearAutomatically}: results are never loaded before this call in the same
     * transaction, and clearing would detach the statement we just locked.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from ReconciliationResult r where r.statement.id = :statementId")
    int deleteByStatementId(@Param("statementId") Long statementId);
}
