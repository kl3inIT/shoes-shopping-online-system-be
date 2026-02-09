package com.sba.ssos.repository;

import com.sba.ssos.entity.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

  Optional<Category> findBySlug(String slug);
}
