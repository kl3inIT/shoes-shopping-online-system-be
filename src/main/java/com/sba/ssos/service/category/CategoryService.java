package com.sba.ssos.service.category;

import com.sba.ssos.dto.request.category.CategoryCreateRequest;
import com.sba.ssos.dto.response.category.CategoryResponse;
import com.sba.ssos.entity.Category;
import com.sba.ssos.exception.base.ConflictException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.CategoryMapper;
import com.sba.ssos.repository.CategoryRepository;
import com.sba.ssos.repository.ShoeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
    CategoryCreateRequest trimmed =
        new CategoryCreateRequest(request.name().trim(), request.description().trim());
    String slug = slugify(trimmed.name());
    if (categoryRepository.findBySlug(slug).isPresent()) {
      throw new ConflictException("error.category.slug.exists", "slug", slug);
    }
    Category category = categoryMapper.toEntity(trimmed);
    category.setSlug(slug);
    category = categoryRepository.save(category);
    return toResponse(category);
  }

  @Transactional
  public CategoryResponse update(UUID id, CategoryCreateRequest request) {
    Category category =
        categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    CategoryCreateRequest trimmed =
        new CategoryCreateRequest(request.name().trim(), request.description().trim());
    String slug = slugify(trimmed.name());
    categoryRepository
        .findBySlug(slug)
        .filter(c -> !c.getId().equals(id))
        .ifPresent(
            c -> {
              throw new ConflictException("error.category.slug.exists", "slug", slug);
            });
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
    categoryRepository.deleteById(id);
  }

  private CategoryResponse toResponse(Category category) {
    long productCount = shoeRepository.countByCategory_Id(category.getId());
    return categoryMapper.toResponse(category, productCount);
  }

  private static String slugify(String name) {
    if (name == null || name.isBlank()) {
      return "";
    }
    return name.trim()
        .toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replaceAll("\\s+", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }
}
