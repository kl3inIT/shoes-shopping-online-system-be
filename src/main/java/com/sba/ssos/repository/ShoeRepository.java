package com.sba.ssos.repository;

import com.sba.ssos.entity.Shoe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface ShoeRepository extends JpaRepository<Shoe, UUID> {

  long countByCategory_Id(UUID categoryId);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);
}
