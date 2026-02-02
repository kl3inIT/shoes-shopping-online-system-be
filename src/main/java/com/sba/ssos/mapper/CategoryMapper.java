package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.category.CategoryCreateRequest;
import com.sba.ssos.dto.response.category.CategoryResponse;
import com.sba.ssos.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

  @Mapping(target = "slug", ignore = true)
  Category toEntity(CategoryCreateRequest request);

  @Mapping(target = "slug", ignore = true)
  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  void updateEntity(@MappingTarget Category category, CategoryCreateRequest request);

  @Mapping(source = "category.id", target = "id")
  @Mapping(source = "category.name", target = "name")
  @Mapping(source = "category.description", target = "description")
  @Mapping(source = "category.slug", target = "slug")
  @Mapping(source = "productCount", target = "productCount")
  @Mapping(source = "category.createdAt", target = "createdAt")
  @Mapping(source = "category.lastUpdatedAt", target = "updatedAt")
  CategoryResponse toResponse(Category category, long productCount);
}
