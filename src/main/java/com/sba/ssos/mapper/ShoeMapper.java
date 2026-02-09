package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.entity.Shoe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShoeMapper {
//
//  @Mapping(target = "slug", ignore = true)
////  @Mapping(target = "category", ignore = true)
//  @Mapping(target = "brand", ignore = true)
//  @Mapping(target = "price", ignore = true)
//  Shoe toEntity(ShoeCreateRequest request);
//
////  @Mapping(source = "category.id", target = "categoryId")
////  @Mapping(source = "category.name", target = "categoryName")
////  @Mapping(source = "brand.id", target = "brandId")
////  @Mapping(source = "brand.name", target = "brandName")
//  @Mapping(source = "createdAt", target = "createdAt")
//  @Mapping(source = "lastUpdatedAt", target = "updatedAt")
////  @Mapping(target = "variants", ignore = true)
//  ShoeResponse toResponse(Shoe shoe);
}