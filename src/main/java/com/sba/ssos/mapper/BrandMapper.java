package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    BrandResponse toResponse(Brand brand);
    
    Brand toEntity(BrandRequest request);
    
    void updateBrandFromRequest(BrandRequest request, @MappingTarget Brand brand);
}
