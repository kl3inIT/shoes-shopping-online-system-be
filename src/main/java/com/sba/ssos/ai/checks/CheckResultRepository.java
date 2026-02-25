package com.sba.ssos.ai.checks;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, UUID> {

  List<CheckResult> findByCheckRunIdOrderByScoreAsc(UUID checkRunId);
}
