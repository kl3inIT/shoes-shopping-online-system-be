package com.sba.ssos.repository.product.shoe;

import com.sba.ssos.dto.request.product.shoe.ShoeStockRequest;
import com.sba.ssos.entity.Shoe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface ShoeRepository extends JpaRepository<Shoe, UUID>, ShoeRepositoryCustom {

  long countByCategory_Id(UUID categoryId);

  boolean existsBySlug(String slug);

  boolean existsBySlugAndIdNot(String slug, UUID id);

  @Query("SELECT COUNT(s) FROM Shoe s WHERE s.brand.id = :brandId")
  long countByBrandId(@Param("brandId") UUID brandId);

  interface BrandCount {
    UUID getBrandId();
    Long getCount();
  }

  @Query("SELECT s.brand.id AS brandId, COUNT(s) AS count FROM Shoe s GROUP BY s.brand.id")
  List<BrandCount> countGroupedByBrandId();

  @Query(
      value = """
          SELECT COUNT(DISTINCT s.id) AS total,
                 COUNT(DISTINCT CASE WHEN sv.active = true AND sv.quantity > 0 THEN s.id END) AS selling,
                 COUNT(DISTINCT CASE WHEN sv.active = true AND sv.quantity = 0 THEN s.id END) AS out_of_stock,
                 COUNT(DISTINCT CASE WHEN sv.active = true AND sv.quantity BETWEEN 1 AND :threshold THEN s.id END) AS low_stock
          FROM shoes s
          LEFT JOIN shoe_variants sv ON sv.shoe_id = s.id
          """,
      nativeQuery = true)
  ShoeStockRequest getStockSummary(@Param("threshold") long threshold);

  default Map<UUID, Long> countPerBrand() {
    return countGroupedByBrandId().stream()
        .collect(Collectors.toMap(BrandCount::getBrandId, BrandCount::getCount));
  }

  Optional<Shoe> findBySlug(String slug);

  @Query("""
      SELECT od.shoeVariant.shoe FROM OrderDetail od
      GROUP BY od.shoeVariant.shoe
      ORDER BY SUM(od.quantity) DESC
      """)
  List<Shoe> findBestSellers(Pageable pageable);
}
