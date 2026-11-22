package com.msetransport.codsettlement.settlement.repository;

import com.msetransport.codsettlement.settlement.domain.StatementFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementFileRepository extends JpaRepository<StatementFile, Long> {
}
