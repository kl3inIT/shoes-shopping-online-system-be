package com.sba.ssos.service.brand;

import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.exception.base.ConflictException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.BrandMapper;
import com.sba.ssos.repository.BrandRepository;
import com.sba.ssos.repository.ShoeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final ShoeRepository shoeRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(brand -> {
                    BrandResponse base = brandMapper.toResponse(brand);
                    long productCount = shoeRepository.countByBrandId(brand.getId());
                    return new BrandResponse(
                            base.id(), base.name(), base.slug(), base.description(),
                            base.logoUrl(), base.country(), base.createdAt(), base.updatedAt(),
                            productCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));
        BrandResponse base = brandMapper.toResponse(brand);
        long productCount = shoeRepository.countByBrandId(brand.getId());
        return new BrandResponse(
                base.id(), base.name(), base.slug(), base.description(),
                base.logoUrl(), base.country(), base.createdAt(), base.updatedAt(),
                productCount);
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
        if (shoeRepository.countByBrandId(id) > 0) {
            throw new ConflictException("error.brand.delete.has_products");
        }
        brandRepository.deleteById(id);
    }

    public Brand findById(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Brand", brandId));
    }
}
