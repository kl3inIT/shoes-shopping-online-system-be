package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    @Mapping(source = "lastUpdatedAt", target = "updatedAt")
    @Mapping(target = "productCount", constant = "0L")
    BrandResponse toResponse(Brand brand);

    Brand toEntity(BrandRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    void updateBrandFromRequest(BrandRequest request, @MappingTarget Brand brand);
}
