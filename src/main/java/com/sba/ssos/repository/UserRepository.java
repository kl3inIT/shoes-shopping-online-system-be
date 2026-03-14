package com.sba.ssos.repository;

import com.sba.ssos.entity.User;
import com.sba.ssos.enums.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  Optional<User> findByKeycloakId(UUID keycloakId);

  Long countByRole(UserRole role);
}
