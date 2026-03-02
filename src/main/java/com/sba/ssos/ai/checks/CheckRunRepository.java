package com.sba.ssos.ai.checks;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckRunRepository extends JpaRepository<CheckRun, UUID> {}
