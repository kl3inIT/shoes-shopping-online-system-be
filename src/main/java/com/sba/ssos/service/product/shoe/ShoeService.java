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
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ShoeService {

    private final ShoeRepository shoeRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ShoeMapper shoeMapper;
    private final ShoeImageService shoeImageService;

    @Transactional
    public ShoeResponse create(ShoeCreateRequest request, List<MultipartFile> shoeImageFiles, List<List<MultipartFile>> variantImageFilesList){
        if (request.variants() != null && request.variants().size() > 80) {
            throw new IllegalArgumentException("Shoe variants must not exceed 80");
        }

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
        for (int i = 0; i < request.variants().size(); i++) {
            ShoeVariantRequest variantRequest = request.variants().get(i);
            String size = variantRequest.size().trim();
            String color = variantRequest.color().trim();
            String baseSku = SkuUtils.buildBaseSku(shoe.getSlug(), size, color);
            String uniqueSku = generateUniqueSku(baseSku);

            ShoeVariant variant = ShoeVariant.builder()
                    .shoe(shoe)
                    .size(size)
                    .color(color)
                    .quantity(variantRequest.quantity())
                    .sku(uniqueSku)
                    .build();

            savedVariants.add(shoeVariantRepository.save(variant));
        }

        List<String> shoeImageUrls = shoeImageService.uploadShoeImages(shoe, savedVariants, shoeImageFiles);
        shoeImageService.uploadVariantImages(shoe, savedVariants, variantImageFilesList);

        List<ShoeVariantResponse> variantResponses = new ArrayList<>();
        for (ShoeVariant variant : savedVariants) {
            List<String> variantImageUrls = shoeImageService.getVariantImageUrls(variant);

            variantResponses.add(new ShoeVariantResponse(
                    variant.getId(),
                    variant.getShoe().getId(),
                    variant.getSize(),
                    variant.getColor(),
                    variant.getQuantity(),
                    variant.getSku(),
                    variantImageUrls,
                    variant.getCreatedAt(),
                    variant.getLastUpdatedAt()
            ));
        }

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
                shoeImageUrls,
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt()
        );
    }

    public List<ShoeResponse> getAll() {
        List<Shoe> shoes = shoeRepository.findAll();
        List<ShoeResponse> shoeResponses = new ArrayList<>();

        for (Shoe shoe : shoes) {
            List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
            List<ShoeVariantResponse> variantResponses = new ArrayList<>();

            // Lấy ảnh của shoe - chỉ lấy từ variant đầu tiên thông qua service
            List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);

            for (ShoeVariant variant : variants) {
                // Lấy ảnh của variant đầu tiên
                List<String> variantImageUrls = shoeImageService.getVariantImageUrls(variant);

                ShoeVariantResponse variantResponse = new ShoeVariantResponse(
                        variant.getId(),
                        variant.getShoe().getId(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getQuantity(),
                        variant.getSku(),
                        variantImageUrls,
                        variant.getCreatedAt(),
                        variant.getLastUpdatedAt()
                );
                variantResponses.add(variantResponse);
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
                    shoeImageUrls,
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt()
            );

            shoeResponses.add(shoeResponse);
        }

        return shoeResponses;
    }

    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(id);

        List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);

        List<ShoeVariantResponse> variantResponses = variants.stream()
                .map(variant -> {
                    List<String> variantImageUrls = shoeImageService.getVariantImageUrls(variant);

                    return new ShoeVariantResponse(
                            variant.getId(),
                            variant.getShoe().getId(),
                            variant.getSize(),
                            variant.getColor(),
                            variant.getQuantity(),
                            variant.getSku(),
                            variantImageUrls,
                            variant.getCreatedAt(),
                            variant.getLastUpdatedAt()
                    );
                })
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
                shoeImageUrls,
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
