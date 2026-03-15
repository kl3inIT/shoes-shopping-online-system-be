package com.sba.ssos.service.brand;

import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.exception.base.ConflictException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.BrandMapper;
import com.sba.ssos.repository.BrandRepository;
import com.sba.ssos.repository.product.shoe.ShoeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {
    private final BrandRepository brandRepository;
    private final ShoeRepository shoeRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        Map<UUID, Long> countPerBrand = shoeRepository.countPerBrand();
        return brandRepository.findAll().stream()
                .map(brand -> {
                    BrandResponse base = brandMapper.toResponse(brand);
                    long productCount = countPerBrand.getOrDefault(brand.getId(), 0L);
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
                .orElseThrow(() -> new NotFoundException("Brand", id));
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
            log.warn("Rejected brand creation because slug {} already exists", request.slug());
            throw new ConflictException("error.brand.slug.exists", "slug", request.slug());
        }
        log.info("Creating brand with slug {}", request.slug());
        Brand brand = brandMapper.toEntity(request);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand", id));

        if (!brand.getSlug().equals(request.slug()) && brandRepository.existsBySlug(request.slug())) {
             log.warn("Rejected brand update for {} because slug {} already exists", id, request.slug());
             throw new ConflictException("error.brand.slug.exists", "slug", request.slug());
        }

        log.info("Updating brand {}", id);
        brandMapper.updateBrandFromRequest(request, brand);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new NotFoundException("Brand", id);
        }
        if (shoeRepository.countByBrandId(id) > 0) {
            log.warn("Rejected brand deletion for {} because products still exist", id);
            throw new ConflictException("error.brand.delete.has_products");
        }
        log.info("Deleting brand {}", id);
        brandRepository.deleteById(id);
    }

    public Brand findById(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Brand", brandId));
    }
}
