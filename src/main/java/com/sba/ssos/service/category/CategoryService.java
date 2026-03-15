package com.sba.ssos.service.category;

import com.sba.ssos.dto.request.category.CategoryCreateRequest;
import com.sba.ssos.dto.response.category.CategoryResponse;
import com.sba.ssos.entity.Category;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.CategoryMapper;
import com.sba.ssos.repository.CategoryRepository;
import com.sba.ssos.repository.product.shoe.ShoeRepository;
import com.sba.ssos.utils.SlugUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ShoeRepository shoeRepository;
  private final CategoryMapper categoryMapper;

  public List<CategoryResponse> getAllCategories() {
    return categoryRepository.findAll().stream().map(this::toResponse).toList();
  }

  public CategoryResponse getById(UUID id) {
    Category category =
        categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    return toResponse(category);
  }

  @Transactional
  public CategoryResponse create(CategoryCreateRequest request) {
    log.info("Creating category {}", request.name());
    CategoryCreateRequest trimmed =
        new CategoryCreateRequest(request.name().trim(), request.description().trim());
    String slug = generateUniqueSlug(SlugUtils.slugify(trimmed.name()), null);
    Category category = categoryMapper.toEntity(trimmed);
    category.setSlug(slug);
    category = categoryRepository.save(category);
    return toResponse(category);
  }

  @Transactional
  public CategoryResponse update(UUID id, CategoryCreateRequest request) {
    log.info("Updating category {}", id);
    Category category =
        categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    CategoryCreateRequest trimmed =
        new CategoryCreateRequest(request.name().trim(), request.description().trim());
    String slug = generateUniqueSlug(SlugUtils.slugify(trimmed.name()), id);
    categoryMapper.updateEntity(category, trimmed);
    category.setSlug(slug);
    category = categoryRepository.save(category);
    return toResponse(category);
  }

  @Transactional
  public void delete(UUID id) {
    if (!categoryRepository.existsById(id)) {
      throw new NotFoundException("Category", id);
    }
    if (shoeRepository.countByCategory_Id(id) > 0) {
      log.warn("Rejected category deletion for {} because products still exist", id);
      throw new com.sba.ssos.exception.base.ConflictException(
          "error.category.delete.has_products");
    }
    log.info("Deleting category {}", id);
    categoryRepository.deleteById(id);
  }

  private CategoryResponse toResponse(Category category) {
    long productCount = shoeRepository.countByCategory_Id(category.getId());
    return categoryMapper.toResponse(category, productCount);
  }

  public Category findById(UUID categoryId){
    return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Category", categoryId));
  }

  private String generateUniqueSlug(String baseSlug, UUID excludeId) {
    if (baseSlug == null || baseSlug.isBlank()) {
      return "";
    }

    String candidate = baseSlug;
    int suffix = 0;

    while (true) {
      Optional<Category> existing = categoryRepository.findBySlug(candidate);
      if (existing.isEmpty()) {
        return candidate;
      }
      if (excludeId != null && existing.get().getId().equals(excludeId)) {
        return candidate;
      }
      suffix++;
      candidate = baseSlug + "-" + suffix;
    }
  }

}
