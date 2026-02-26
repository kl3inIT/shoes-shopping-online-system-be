package com.sba.ssos.service.product.shoe;

import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeUpdateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeStatusUpdateRequest;
import com.sba.ssos.dto.request.product.shoevariant.ShoeVariantRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.entity.Category;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.exception.base.ConflictException;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.ShoeMapper;
import com.sba.ssos.repository.CartItemRepository;
import com.sba.ssos.repository.ReviewRepository;
import com.sba.ssos.repository.ShoeImageRepository;
import com.sba.ssos.repository.WishlistRepository;
import com.sba.ssos.repository.order.OrderDetailRepository;
import com.sba.ssos.repository.product.shoe.ShoeRepository;
import com.sba.ssos.repository.ShoeVariantRepository;
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import com.sba.ssos.utils.SkuUtils;
import com.sba.ssos.service.brand.BrandService;
import com.sba.ssos.service.category.CategoryService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.sba.ssos.utils.SlugUtils.slugify;

@Service
@RequiredArgsConstructor
public class ShoeService {

    private final ShoeRepository shoeRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;
    private final ShoeImageRepository shoeImageRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ShoeMapper shoeMapper;
    private final ShoeImageService shoeImageService;

    @Transactional
    public ShoeResponse create(ShoeCreateRequest request, List<MultipartFile> shoeImageFiles, List<List<MultipartFile>> variantImageFilesList) {
        if (request.variants() != null && request.variants().size() > 80) {
            throw new IllegalArgumentException("Shoe variants must not exceed 80");
        }

        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        String name = request.name().trim();
        String description = request.description().trim();
        String material = request.material().trim();
        String slug = generateUniqueSlug(slugify(name), null);

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
                shoe.getCategory().getSlug(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getBrand().getSlug(),
                shoe.getPrice(),
                shoeImageUrls,
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt(),
                shoe.isDeleted()
        );
    }

    public List<ShoeResponse> getAllNotDeleted() {
        List<Shoe> shoes = shoeRepository.findByDeletedFalse();
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
                    shoe.getCategory().getSlug(),
                    shoe.getBrand().getId(),
                    shoe.getBrand().getName(),
                    shoe.getBrand().getSlug(),
                    shoe.getPrice(),
                    shoeImageUrls,
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt(),
                    shoe.isDeleted()
            );

            shoeResponses.add(shoeResponse);
        }

        return shoeResponses;
    }

    public List<ShoeResponse> getAllForAdmin() {
        List<Shoe> shoes = shoeRepository.findAll();
        List<ShoeResponse> shoeResponses = new ArrayList<>();

        for (Shoe shoe : shoes) {
            List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
            List<ShoeVariantResponse> variantResponses = new ArrayList<>();

            List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);

            for (ShoeVariant variant : variants) {
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
                    shoe.getCategory().getSlug(),
                    shoe.getBrand().getId(),
                    shoe.getBrand().getName(),
                    shoe.getBrand().getSlug(),
                    shoe.getPrice(),
                    shoeImageUrls,
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt(),
                    shoe.isDeleted()
            );

            shoeResponses.add(shoeResponse);
        }

        return shoeResponses;
    }

    public List<ShoeResponse> getAllDeleted() {
        List<Shoe> shoes = shoeRepository.findByDeletedTrue();
        List<ShoeResponse> shoeResponses = new ArrayList<>();

        for (Shoe shoe : shoes) {
            List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
            List<ShoeVariantResponse> variantResponses = new ArrayList<>();

            List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);

            for (ShoeVariant variant : variants) {
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
                    shoe.getCategory().getSlug(),
                    shoe.getBrand().getId(),
                    shoe.getBrand().getName(),
                    shoe.getBrand().getSlug(),
                    shoe.getPrice(),
                    shoeImageUrls,
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt(),
                    shoe.isDeleted()
            );

            shoeResponses.add(shoeResponse);
        }

        return shoeResponses;
    }

    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findByIdAndDeletedFalse(id)
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
                shoe.getCategory().getSlug(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getBrand().getSlug(),
                shoe.getPrice(),
                shoeImageUrls,
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt(),
                shoe.isDeleted()
        );
    }

    @Transactional
    public ShoeResponse update(
            UUID id,
            ShoeUpdateRequest request,
            List<MultipartFile> shoeImageFiles,
            List<List<MultipartFile>> variantImageFilesList
    ) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        String name = request.name().trim();
        String description = request.description().trim();
        String material = request.material().trim();
        String slug = generateUniqueSlug(slugify(name), shoe.getId());

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

        List<ShoeVariant> existingVariants = shoeVariantRepository.findByShoe_Id(shoe.getId());
        shoeVariantRepository.deleteAll(existingVariants);

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

        if (shoeImageFiles != null && !shoeImageFiles.isEmpty()) {
            shoeImageService.uploadShoeImages(shoe, savedVariants, shoeImageFiles);
        }
        if (variantImageFilesList != null && !variantImageFilesList.isEmpty()) {
            shoeImageService.uploadVariantImages(shoe, savedVariants, variantImageFilesList);
        }

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

        List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, savedVariants);

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
                shoe.getCategory().getSlug(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getBrand().getSlug(),
                shoe.getPrice(),
                shoeImageUrls,
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt(),
                shoe.isDeleted()
        );
    }

    @Transactional
    public void delete(UUID id) {
        Shoe shoe = shoeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));
        shoe.setDeleted(true);
        shoeRepository.save(shoe);
    }

    @Transactional
    public ShoeResponse updateStatus(UUID id, ShoeStatusUpdateRequest request) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        shoe.setStatus(request.status());
        shoeRepository.save(shoe);

        return getById(id);
    }

    public Page<ShoeResponse> search(
            String search,
            List<UUID> brandIds,
            List<String> sizes,
            List<UUID> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> statuses,
            List<String> genders,
            Pageable pageable
    ) {
        Page<Shoe> page = shoeRepository.searchShoes(
                search,
                brandIds,
                sizes,
                categoryIds,
                minPrice,
                maxPrice,
                statuses,
                genders,
                pageable
        );

        return page.map(shoe -> {
            List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
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
                    shoe.getCategory().getSlug(),
                    shoe.getBrand().getId(),
                    shoe.getBrand().getName(),
                    shoe.getBrand().getSlug(),
                    shoe.getPrice(),
                    shoeImageUrls,
                    variantResponses,
                    shoe.getCreatedAt(),
                    shoe.getLastUpdatedAt(),
                    shoe.isDeleted()
            );
        });
    }

    public List<ShoeResponse> getNewArrivals(int limit) {
        List<Shoe> shoes = shoeRepository.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return shoes.stream().map(this::toShoeResponse).toList();
    }

    public List<ShoeResponse> getBestSellers(int limit) {
        List<Shoe> shoes = shoeRepository.findBestSellers(PageRequest.of(0, limit));
        if (shoes.size() < limit) {
            List<UUID> existingIds = shoes.stream().map(Shoe::getId).toList();
            List<Shoe> fallback = shoeRepository.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, limit));
            for (Shoe s : fallback) {
                if (shoes.size() >= limit) break;
                if (!existingIds.contains(s.getId())) {
                    shoes.add(s);
                }
            }
        }
        return shoes.stream().map(this::toShoeResponse).toList();
    }

    private ShoeResponse toShoeResponse(Shoe shoe) {
        List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(shoe.getId());
        List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);

        List<ShoeVariantResponse> variantResponses = variants.stream()
                .map(variant -> new ShoeVariantResponse(
                        variant.getId(),
                        variant.getShoe().getId(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getQuantity(),
                        variant.getSku(),
                        shoeImageService.getVariantImageUrls(variant),
                        variant.getCreatedAt(),
                        variant.getLastUpdatedAt()
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
                shoe.getCategory().getSlug(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getBrand().getSlug(),
                shoe.getPrice(),
                shoeImageUrls,
                variantResponses,
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt(),
                shoe.isDeleted()
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

    @Transactional
    public ShoeResponse restore(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        if (!shoe.isDeleted()) {
            return getById(id);
        }

        shoe.setDeleted(false);
        shoeRepository.save(shoe);

        return getById(id);
    }

    @Transactional
    public void forceDelete(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        boolean hasAnyOrders = orderDetailRepository.existsByShoeVariant_Shoe_Id(id);

        if (hasAnyOrders) {
            throw new ConflictException(
                    "error.shoe.cannot.delete.has.orders",
                    "shoeId", id
            );
        }

        boolean hasReviews = reviewRepository.existsByShoeVariant_Shoe_Id(id);
        if (hasReviews) {
            throw new ConflictException(
                    "error.shoe.cannot.delete.has.reviews",
                    "shoeId", id
            );
        }

        // Hard delete nếu có Order hay review thì ko được
        cartItemRepository.deleteAllByShoeVariant_Shoe_Id(id);
        wishlistRepository.deleteAllByShoe_Id(id);
        shoeImageRepository.deleteAllByShoe_Id(id);

        List<ShoeVariant> variants = shoeVariantRepository.findByShoe_Id(id);
        shoeVariantRepository.deleteAll(variants);

        shoeRepository.delete(shoe);
    }

}
