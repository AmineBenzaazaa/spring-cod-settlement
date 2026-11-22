package com.msetransport.codsettlement.settlement.repository;

import com.msetransport.codsettlement.settlement.domain.Dispute;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {

    @Override
    @EntityGraph(attributePaths = "result")
    Page<Dispute> findAll(Specification<Dispute> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "result")
    Optional<Dispute> findOne(Specification<Dispute> spec);

    /** The export prints merchant and courier names: fetch the whole graph in one query. */
    @Override
    @EntityGraph(attributePaths = {"merchant", "result", "result.statement", "result.statement.courier"})
    List<Dispute> findAll(Specification<Dispute> spec, Sort sort);

    boolean existsByResultId(Long resultId);

    boolean existsByResultStatementId(Long statementId);

    /** Open disputes across all merchants (staff overview): how many, and how much is claimed. */
    @Query("""
            select new com.msetransport.codsettlement.settlement.repository.CountAndSum(count(d), sum(d.claimedAmount))
            from Dispute d
            where d.status = com.msetransport.codsettlement.settlement.domain.DisputeStatus.OPEN""")
    CountAndSum openTotals();

    @Query("""
            select new com.msetransport.codsettlement.settlement.repository.CountAndSum(count(d), sum(d.claimedAmount))
            from Dispute d
            where d.status = com.msetransport.codsettlement.settlement.domain.DisputeStatus.OPEN
              and d.merchant.id = :merchantId""")
    CountAndSum openTotals(@Param("merchantId") Long merchantId);

    @Query("select d.result.id from Dispute d where d.result.statement.id = :statementId")
    Set<Long> findResultIdsByStatementId(@Param("statementId") Long statementId);
}
