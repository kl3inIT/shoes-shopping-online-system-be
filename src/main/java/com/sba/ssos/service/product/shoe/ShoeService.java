package com.sba.ssos.service.product.shoe;

import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.request.product.shoevariant.ShoeVariantRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.entity.Category;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.ShoeMapper;
import com.sba.ssos.repository.ShoeRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.utils.SlugUtils;
import com.sba.ssos.utils.SkuUtils;
import com.sba.ssos.service.brand.BrandService;
import com.sba.ssos.service.category.CategoryService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShoeService {

    private final ShoeRepository shoeRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ShoeMapper shoeMapper;

    @Transactional
    public ShoeResponse create(ShoeCreateRequest request){
        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        String name = request.name().trim();
        String description = request.description().trim();
        String material = request.material().trim();
        String slug = generateUniqueSlug(SlugUtils.slugify(name), null);

        Shoe shoe = new Shoe();
        shoe.setName(name);
        shoe.setSlug(slug);
        shoe.setGender(request.gender());
        shoe.setStatus(request.status());
        shoe.setDescription(description);
        shoe.setMaterial(material);
        shoe.setPrice(request.price());
        shoe.setCategory(category);
        shoe.setBrand(brand);

        shoeRepository.save(shoe);

        List<ShoeVariant> savedVariants = new ArrayList<>();
        for (ShoeVariantRequest vr : request.variants()) {
            String size = vr.size().trim();
            String color = vr.color().trim();
            String baseSku = SkuUtils.buildBaseSku(shoe.getSlug(), size, color);
            String uniqueSku = generateUniqueSku(baseSku);

            ShoeVariant variant = ShoeVariant.builder()
                    .shoe(shoe)
                    .size(size)
                    .color(color)
                    .quantity(vr.quantity())
                    .sku(uniqueSku)
                    .build();

            savedVariants.add(shoeVariantRepository.save(variant));
        }

        List<ShoeVariantResponse> variantResponses = savedVariants.stream()
                .map(v -> new ShoeVariantResponse(
                        v.getId(),
                        v.getShoe().getId(),
                        v.getSize(),
                        v.getColor(),
                        v.getQuantity(),
                        v.getSku(),
                        v.getCreatedAt(),
                        v.getLastUpdatedAt()
                ))
                .toList();

        return new ShoeResponse(
                shoe.getId(),
                shoe.getName(),
                shoe.getDescription(),
                shoe.getSlug(),
                shoe.getMaterial(),
                shoe.getGender(),
                shoe.getStatus(),
                shoe.getCategory().getId(),
                shoe.getCategory().getName(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getPrice(),
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt()
        );
    }

    public List<ShoeResponse> getAll() {
        List<Shoe> shoes = shoeRepository.findAll();
        List<ShoeResponse> responses = new ArrayList<>();

        for (Shoe shoe : shoes) {
            List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
            List<ShoeVariantResponse> variantResponses = new ArrayList<>();

            for (ShoeVariant v : variants) {
                ShoeVariantResponse vr = new ShoeVariantResponse(
                        v.getId(),
                        v.getShoe().getId(),
                        v.getSize(),
                        v.getColor(),
                        v.getQuantity(),
                        v.getSku(),
                        v.getCreatedAt(),
                        v.getLastUpdatedAt()
                );
                variantResponses.add(vr);
            }

            ShoeResponse shoeResponse = new ShoeResponse(
                    shoe.getId(),
                    shoe.getName(),
                    shoe.getDescription(),
                    shoe.getSlug(),
                    shoe.getMaterial(),
                    shoe.getGender(),
                    shoe.getStatus(),
                    shoe.getCategory().getId(),
                    shoe.getCategory().getName(),
                    shoe.getBrand().getId(),
                    shoe.getBrand().getName(),
                    shoe.getPrice(),
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt()
            );

            responses.add(shoeResponse);
        }

        return responses;
    }

    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(id);
        List<ShoeVariantResponse> variantResponses = variants.stream()
                .map(v -> new ShoeVariantResponse(
                        v.getId(),
                        v.getShoe().getId(),
                        v.getSize(),
                        v.getColor(),
                        v.getQuantity(),
                        v.getSku(),
                        v.getCreatedAt(),
                        v.getLastUpdatedAt()
                ))
                .toList();

        return new ShoeResponse(
                shoe.getId(),
                shoe.getName(),
                shoe.getDescription(),
                shoe.getSlug(),
                shoe.getMaterial(),
                shoe.getGender(),
                shoe.getStatus(),
                shoe.getCategory().getId(),
                shoe.getCategory().getName(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getPrice(),
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt()
        );
    }


  private String generateUniqueSlug(String baseSlug, UUID excludeId) {
    if (baseSlug == null || baseSlug.isBlank()) {
      return "";
    }

    String candidate = baseSlug;
    int suffix = 0;

    while (true) {
      boolean exists;
      if (excludeId != null) {
        exists = shoeRepository.existsBySlugAndIdNot(candidate, excludeId);
      } else {
        exists = shoeRepository.existsBySlug(candidate);
      }

      if (!exists) {
        return candidate;
      }

      suffix++;
      candidate = baseSlug + "-" + suffix;
    }
  }

    private String generateUniqueSku(String baseSku) {
        String candidate = baseSku;
        int suffix = 0;

        while (shoeVariantRepository.existsBySku(candidate)) {
            suffix++;
            candidate = SkuUtils.appendNumericSuffix(baseSku, suffix);
        }

        return candidate;
    }
}
