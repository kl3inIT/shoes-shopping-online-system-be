package com.sba.ssos.repository;

import com.sba.ssos.entity.Shoe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface ShoeRepository extends JpaRepository<Shoe, UUID> {

  long countByCategory_Id(UUID categoryId);

  @Query("SELECT COUNT(s) FROM Shoe s WHERE s.brand.id = :brandId")
  long countByBrandId(@Param("brandId") UUID brandId);

  interface BrandCount {
    UUID getBrandId();
    Long getCount();
  }

  @Query("SELECT s.brand.id AS brandId, COUNT(s) AS count FROM Shoe s GROUP BY s.brand.id")
  List<BrandCount> countGroupedByBrandId();

  default Map<UUID, Long> countPerBrand() {
    return countGroupedByBrandId().stream()
        .collect(Collectors.toMap(BrandCount::getBrandId, BrandCount::getCount));
  }

  Optional<Shoe> findBySlug(String slug);
}
