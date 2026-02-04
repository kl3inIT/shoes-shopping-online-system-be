package com.sba.ssos.service;

import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.exception.base.ConflictException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.BrandMapper;
import com.sba.ssos.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Brand with slug '" + request.slug() + "' already exists");
        }
        Brand brand = brandMapper.toEntity(request);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));

        if (!brand.getSlug().equals(request.slug()) && brandRepository.existsBySlug(request.slug())) {
             throw new ConflictException("Brand with slug '" + request.slug() + "' already exists");
        }
        
        brandMapper.updateBrandFromRequest(request, brand);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new NotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }
}
