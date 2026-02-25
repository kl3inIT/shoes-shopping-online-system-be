package com.sba.ssos.ai.parameters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AiParametersRepository extends JpaRepository<AiParameters, UUID> {

  Optional<AiParameters> findFirstByActiveTrueAndTargetType(AiParameters.TargetType targetType);

  List<AiParameters> findByTargetTypeOrderByCreatedAtDesc(AiParameters.TargetType targetType);

  @Modifying
  @Query("UPDATE AiParameters p SET p.active = false WHERE p.targetType = :type AND p.id <> :id")
  void deactivateAllExcept(AiParameters.TargetType type, UUID id);
}
