package com.sba.ssos.repository;

import com.sba.ssos.entity.Shoe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoeRepository extends JpaRepository<Shoe, UUID> {

  long countByCategory_Id(UUID categoryId);

  @Query("SELECT COUNT(s) FROM Shoe s WHERE s.brand.id = :brandId")
  long countByBrandId(@Param("brandId") UUID brandId);

  Optional<Shoe> findBySlug(String slug);
}
