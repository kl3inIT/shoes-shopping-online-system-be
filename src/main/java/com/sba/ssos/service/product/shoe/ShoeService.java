package com.sba.ssos.service.product.shoe;

import com.sba.ssos.dto.request.product.shoe.*;
import com.sba.ssos.dto.request.product.shoevariant.ShoeVariantRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoe.ShoeStockSummaryResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.entity.*;
import com.sba.ssos.enums.*;
import com.sba.ssos.exception.base.*;
import com.sba.ssos.repository.*;
import com.sba.ssos.repository.order.OrderDetailRepository;
import com.sba.ssos.repository.product.shoe.ShoeRepository;
import com.sba.ssos.service.brand.BrandService;
import com.sba.ssos.service.category.CategoryService;
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import com.sba.ssos.utils.SkuUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

import static com.sba.ssos.utils.SlugUtils.slugify;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoeService {

    private final ShoeRepository shoeRepository;
    private final ShoeVariantRepository shoeVariantRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItemRepository;
    private final ShoeImageRepository shoeImageRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ShoeImageService shoeImageService;

    @Transactional
    public ShoeResponse create(ShoeCreateRequest request, List<MultipartFile> shoeImageFiles, List<List<MultipartFile>> variantImageFilesList) {
        if (request.variants() != null && request.variants().size() > 80) {
            log.warn("Rejected shoe creation because variant count {} exceeds limit", request.variants().size());
            throw new BadRequestException("error.shoe.variants.max_count", "maxCount", 80);
        }

        log.info("Creating shoe {}", request.name());

        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        Shoe shoe = new Shoe();
        updateShoeBaseInfo(shoe, request, category, brand, null);
        shoeRepository.save(shoe);

        List<ShoeVariant> savedVariants = createVariants(shoe, request.variants());

        List<String> shoeImageUrls = shoeImageService.uploadShoeImages(shoe, savedVariants, shoeImageFiles);
        shoeImageService.uploadVariantImages(shoe, savedVariants, variantImageFilesList);

        return buildShoeResponse(shoe, savedVariants, shoeImageUrls);
    }

    @Transactional(readOnly = true)
    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        return toShoeResponses(List.of(shoe)).get(0);
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

        log.info("Updating shoe {}", id);

        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        updateShoeBaseInfo(shoe, request, category, brand, shoe.getId());
        shoeRepository.save(shoe);

        List<ShoeVariant> existingVariants = shoeVariantRepository.findByShoe_Id(shoe.getId());
        Map<UUID, ShoeVariant> existingVariantById = new HashMap<>();
        Map<String, ShoeVariant> existingVariantByKey = new HashMap<>();
        indexExistingVariants(existingVariants, existingVariantById, existingVariantByKey);

        validateNoDuplicateVariants(request.variants(), id);

        Set<UUID> touchedVariantIds = new HashSet<>();
        List<ShoeVariant> variantsInRequestOrder = new ArrayList<>();

        upsertVariants(
                id,
                shoe,
                request.variants(),
                existingVariantById,
                existingVariantByKey,
                touchedVariantIds,
                variantsInRequestOrder
        );

        handleUntouchedVariants(existingVariants, touchedVariantIds);

        syncImagesAfterUpdate(shoe, request, shoeImageFiles, variantImageFilesList, variantsInRequestOrder);

        return getById(id);
    }

    @Transactional(readOnly = true)
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
        validatePageable(pageable);
        validatePriceRange(minPrice, maxPrice);
        long startedAt = System.nanoTime();

        List<String> normalizedStatuses = normalizeStatuses(statuses);
        List<String> normalizedGenders = normalizeGenders(genders);

        Page<Shoe> page = shoeRepository.searchShoes(
                search,
                brandIds,
                sizes,
                categoryIds,
                minPrice,
                maxPrice,
                normalizedStatuses,
                normalizedGenders,
                pageable
        );

        List<ShoeResponse> content = toShoeResponses(page.getContent());
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        if (durationMs > 1_000) {
            log.warn(
                    "Slow shoe search detected: {} ms (page={}, size={}, total={}, search='{}', brands={}, categories={}, sizes={}, statuses={}, genders={})",
                    durationMs,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    page.getTotalElements(),
                    search,
                    brandIds == null ? 0 : brandIds.size(),
                    categoryIds == null ? 0 : categoryIds.size(),
                    sizes == null ? 0 : sizes.size(),
                    normalizedStatuses == null ? 0 : normalizedStatuses.size(),
                    normalizedGenders == null ? 0 : normalizedGenders.size()
            );
        }

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ShoeResponse> getNewArrivals(int limit) {
        List<Shoe> shoes = shoeRepository.findAll(PageRequest.of(0, limit)).getContent();
        return toShoeResponses(shoes);
    }

    @Transactional(readOnly = true)
    public List<ShoeResponse> getBestSellers(int limit) {
        List<Shoe> shoes = shoeRepository.findBestSellers(PageRequest.of(0, limit));
        if (shoes.size() < limit) {
            List<UUID> existingIds = shoes.stream().map(Shoe::getId).toList();
            List<Shoe> fallback = shoeRepository.findAll(PageRequest.of(0, limit)).getContent();
            for (Shoe s : fallback) {
                if (shoes.size() >= limit) break;
                if (!existingIds.contains(s.getId())) {
                    shoes.add(s);
                }
            }
        }
        return toShoeResponses(shoes);
    }

    @Transactional(readOnly = true)
    public ShoeStockSummaryResponse getStockSummary(long threshold) {
        ShoeStockRequest summary = shoeRepository.getStockSummary(threshold);
        if (summary == null) {
            return new ShoeStockSummaryResponse(0L, 0L, 0L, 0L);
        }

        return new ShoeStockSummaryResponse(
                Optional.of(summary.total()).orElse(0L),
                Optional.of(summary.selling()).orElse(0L),
                Optional.of(summary.outOfStock()).orElse(0L),
                Optional.of(summary.lowStock()).orElse(0L)
        );
    }

    private ShoeResponse toShoeResponse(Shoe shoe) {
        List<ShoeVariant> variants = shoeVariantRepository.findByShoe_IdAndActiveTrueOrderBySizeAscColorAsc(shoe.getId());
        List<String> shoeImageUrls = shoeImageService.getShoeImageUrls(shoe, variants);
        return buildShoeResponse(shoe, variants, shoeImageUrls);
    }

    private List<ShoeResponse> toShoeResponses(List<Shoe> shoes) {
        if (shoes == null || shoes.isEmpty()) {
            return List.of();
        }

        List<UUID> shoeIds = shoes.stream().map(Shoe::getId).toList();
        List<ShoeVariant> variants =
                shoeVariantRepository.findByShoe_IdInAndActiveTrueOrderByShoe_IdAscSizeAscColorAsc(shoeIds);
        Map<UUID, List<ShoeVariant>> variantsByShoeId = new LinkedHashMap<>();
        for (Shoe shoe : shoes) {
            variantsByShoeId.put(shoe.getId(), new ArrayList<>());
        }
        for (ShoeVariant variant : variants) {
            variantsByShoeId.computeIfAbsent(variant.getShoe().getId(), unused -> new ArrayList<>())
                    .add(variant);
        }

        List<UUID> variantIds = variants.stream().map(ShoeVariant::getId).toList();
        Map<UUID, List<String>> shoeImageUrlsByShoeId = shoeImageService.getShoeImageUrlsByShoeIds(shoeIds);
        Map<UUID, List<String>> variantImageUrlsByVariantId =
                shoeImageService.getVariantImageUrlsByVariantIds(variantIds);

        Map<UUID, ShoeReviewSnapshot> reviewStatsByShoeId = new HashMap<>();
        for (ReviewRepository.ShoeReviewStats stats : reviewRepository.getStatsByShoeIds(shoeIds)) {
            reviewStatsByShoeId.put(
                    stats.getShoeId(),
                    new ShoeReviewSnapshot(stats.getAverageStars(), stats.getReviewCount())
            );
        }

        return shoes.stream()
                .map(shoe -> buildShoeResponse(
                        shoe,
                        variantsByShoeId.getOrDefault(shoe.getId(), List.of()),
                        shoeImageUrlsByShoeId.getOrDefault(shoe.getId(), List.of()),
                        reviewStatsByShoeId.getOrDefault(shoe.getId(), ShoeReviewSnapshot.EMPTY),
                        variantImageUrlsByVariantId
                ))
                .toList();
    }

    private ShoeResponse buildShoeResponse(Shoe shoe, List<ShoeVariant> variants, List<String> shoeImageUrls) {
        Double avgRating = reviewRepository.getAverageStarsByShoeId(shoe.getId());
        long reviewCount = reviewRepository.countByShoeVariant_Shoe_Id(shoe.getId());

        return buildShoeResponse(
                shoe,
                variants,
                shoeImageUrls,
                new ShoeReviewSnapshot(avgRating, reviewCount),
                Map.of()
        );
    }

    private ShoeResponse buildShoeResponse(
            Shoe shoe,
            List<ShoeVariant> variants,
            List<String> shoeImageUrls,
            ShoeReviewSnapshot reviewSnapshot,
            Map<UUID, List<String>> variantImageUrlsByVariantId
    ) {

        return ShoeResponse.builder()
                .id(shoe.getId())
                .name(shoe.getName())
                .description(shoe.getDescription())
                .slug(shoe.getSlug())
                .material(shoe.getMaterial())
                .gender(shoe.getGender())
                .status(shoe.getStatus())
                .categoryId(shoe.getCategory().getId())
                .categoryName(shoe.getCategory().getName())
                .categorySlug(shoe.getCategory().getSlug())
                .brandId(shoe.getBrand().getId())
                .brandName(shoe.getBrand().getName())
                .brandSlug(shoe.getBrand().getSlug())
                .price(shoe.getPrice())
                .avgRating(reviewSnapshot.avgRating())
                .reviewCount(reviewSnapshot.reviewCount())
                .imageUrls(shoeImageUrls)
                .variants(toVariantResponses(variants, variantImageUrlsByVariantId))
                .createdAt(shoe.getCreatedAt())
                .updatedAt(shoe.getLastUpdatedAt())
                .build();
    }

    private List<ShoeVariantResponse> toVariantResponses(List<ShoeVariant> variants) {
        return toVariantResponses(variants, Map.of());
    }

    private List<ShoeVariantResponse> toVariantResponses(
            List<ShoeVariant> variants,
            Map<UUID, List<String>> variantImageUrlsByVariantId
    ) {
        return variants.stream()
                .map(variant -> new ShoeVariantResponse(
                        variant.getId(),
                        variant.getShoe().getId(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getQuantity(),
                        variant.getSku(),
                        variantImageUrlsByVariantId.isEmpty()
                                ? shoeImageService.getVariantImageUrls(variant)
                                : variantImageUrlsByVariantId.getOrDefault(variant.getId(), List.of()),
                        variant.getCreatedAt(),
                        variant.getLastUpdatedAt()
                ))
                .toList();
    }

    private record ShoeReviewSnapshot(Double avgRating, long reviewCount) {
        private static final ShoeReviewSnapshot EMPTY = new ShoeReviewSnapshot(null, 0L);
    }

    private List<ShoeVariant> createVariants(Shoe shoe, List<ShoeVariantRequest> requests) {
        List<ShoeVariant> savedVariants = new ArrayList<>();
        for (ShoeVariantRequest variantRequest : requests) {
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
        return savedVariants;
    }

    private void updateShoeBaseInfo(Shoe shoe, ShoeCreateRequest request, Category category, Brand brand, UUID excludeId) {
        applyShoeBaseInfo(
                shoe,
                request.name(),
                request.description(),
                request.material(),
                request.gender(),
                request.status(),
                request.price(),
                category,
                brand,
                excludeId
        );
    }

    private void updateShoeBaseInfo(Shoe shoe, ShoeUpdateRequest request, Category category, Brand brand, UUID excludeId) {
        applyShoeBaseInfo(
                shoe,
                request.name(),
                request.description(),
                request.material(),
                request.gender(),
                request.status(),
                request.price(),
                category,
                brand,
                excludeId
        );
    }

    private void applyShoeBaseInfo(
            Shoe shoe,
            String nameValue,
            String descriptionValue,
            String materialValue,
            Gender gender,
            ShoeStatus status,
            Double price,
            Category category,
            Brand brand,
            UUID excludeId
    ) {
        String name = nameValue.trim();
        String description = descriptionValue.trim();
        String material = materialValue.trim();
        String slug = generateUniqueSlug(slugify(name), excludeId);

        shoe.setName(name);
        shoe.setSlug(slug);
        shoe.setGender(gender);
        shoe.setStatus(status);
        shoe.setDescription(description);
        shoe.setMaterial(material);
        shoe.setPrice(price);
        shoe.setCategory(category);
        shoe.setBrand(brand);
    }

    private void indexExistingVariants(
            List<ShoeVariant> existingVariants,
            Map<UUID, ShoeVariant> existingVariantById,
            Map<String, ShoeVariant> existingVariantByKey
    ) {
        for (ShoeVariant variant : existingVariants) {
            existingVariantById.put(variant.getId(), variant);
            existingVariantByKey.put(toVariantKey(variant.getSize(), variant.getColor()), variant);
        }
    }

    private void validateNoDuplicateVariants(List<ShoeVariantRequest> variants, UUID shoeId) {
        Set<String> seenKeys = new HashSet<>();
        for (ShoeVariantRequest variant : variants) {
            String key = toVariantKey(variant.size(), variant.color());
            if (!seenKeys.add(key)) {
                throw new ConflictException(
                        "error.shoe.variant.duplicate",
                        "shoeId", shoeId,
                        "size", variant.size(),
                        "color", variant.color()
                );
            }
        }
    }

    private void upsertVariants(
            UUID shoeId,
            Shoe shoe,
            List<ShoeVariantRequest> requests,
            Map<UUID, ShoeVariant> existingVariantById,
            Map<String, ShoeVariant> existingVariantByKey,
            Set<UUID> touchedVariantIds,
            List<ShoeVariant> variantsInRequestOrder
    ) {
        for (ShoeVariantRequest request : requests) {
            ShoeVariant variant = resolveVariant(request, existingVariantById, existingVariantByKey);
            if (variant == null) {
                variant = createVariantFromRequest(shoe, request);
            } else {
                updateExistingVariant(shoeId, shoe, variant, request, existingVariantByKey);
            }

            variant = shoeVariantRepository.save(variant);
            touchedVariantIds.add(variant.getId());
            variantsInRequestOrder.add(variant);
        }
    }

    private ShoeVariant resolveVariant(
            ShoeVariantRequest request,
            Map<UUID, ShoeVariant> existingVariantById,
            Map<String, ShoeVariant> existingVariantByKey
    ) {
        if (request.id() != null) {
            ShoeVariant variant = existingVariantById.get(request.id());
            if (variant == null) {
                throw new NotFoundException("ShoeVariant", request.id());
            }
            return variant;
        }

        String key = toVariantKey(request.size(), request.color());
        return existingVariantByKey.get(key);
    }

    private ShoeVariant createVariantFromRequest(Shoe shoe, ShoeVariantRequest request) {
        String size = request.size().trim();
        String color = request.color().trim();
        boolean active = request.active() == null || request.active();

        String baseSku = SkuUtils.buildBaseSku(shoe.getSlug(), size, color);
        String uniqueSku = generateUniqueSku(baseSku);

        return ShoeVariant.builder()
                .shoe(shoe)
                .size(size)
                .color(color)
                .quantity(request.quantity())
                .sku(uniqueSku)
                .active(active)
                .build();
    }

    private void updateExistingVariant(
            UUID shoeId,
            Shoe shoe,
            ShoeVariant variant,
            ShoeVariantRequest request,
            Map<String, ShoeVariant> existingVariantByKey
    ) {
        String requestedSize = request.size().trim();
        String requestedColor = request.color().trim();
        Long requestedQuantity = request.quantity();
        boolean requestedActive = request.active() == null || request.active();
        String requestedKey = toVariantKey(requestedSize, requestedColor);

        boolean isUsed = isVariantUsed(variant.getId());
        boolean sizeChanged = !variant.getSize().equalsIgnoreCase(requestedSize);
        boolean colorChanged = !variant.getColor().equalsIgnoreCase(requestedColor);

        if (isUsed && (sizeChanged || colorChanged)) {
            throw new ConflictException(
                    "error.shoe.variant.cannot.modify.used",
                    "shoeId", shoeId,
                    "variantId", variant.getId()
            );
        }

        if (!isUsed) {
            ShoeVariant conflict = existingVariantByKey.get(requestedKey);
            if (conflict != null && !conflict.getId().equals(variant.getId())) {
                throw new ConflictException(
                        "error.shoe.variant.duplicate",
                        "shoeId", shoeId,
                        "size", requestedSize,
                        "color", requestedColor
                );
            }

            if (sizeChanged || colorChanged) {
                variant.setSize(requestedSize);
                variant.setColor(requestedColor);

                String baseSku = SkuUtils.buildBaseSku(shoe.getSlug(), requestedSize, requestedColor);
                String uniqueSku = generateUniqueSkuForUpdate(baseSku, variant.getId());
                variant.setSku(uniqueSku);
            }
        }

        variant.setQuantity(requestedQuantity);
        variant.setActive(requestedActive);
    }

    private void handleUntouchedVariants(List<ShoeVariant> existingVariants, Set<UUID> touchedVariantIds) {
        for (ShoeVariant existingVariant : existingVariants) {
            if (touchedVariantIds.contains(existingVariant.getId())) {
                continue;
            }

            if (isVariantUsed(existingVariant.getId())) {
                existingVariant.setActive(false);
                existingVariant.setQuantity(0L);
                shoeVariantRepository.save(existingVariant);
                continue;
            }

            cartItemRepository.deleteAllByShoeVariant_Id(existingVariant.getId());
            shoeImageRepository.deleteAllByShoeVariant_Id(existingVariant.getId());
            shoeVariantRepository.delete(existingVariant);
        }
    }

    private void syncImagesAfterUpdate(
            Shoe shoe,
            ShoeUpdateRequest request,
            List<MultipartFile> shoeImageFiles,
            List<List<MultipartFile>> variantImageFilesList,
            List<ShoeVariant> variantsInRequestOrder
    ) {
        shoeImageService.syncShoeImagesForUpdate(
                shoe,
                request.keepShoeImageUrls(),
                shoeImageFiles
        );

        shoeImageService.syncVariantImagesForUpdate(
                shoe,
                variantsInRequestOrder,
                request.variantImageUpdates(),
                variantImageFilesList
        );
    }

    private boolean isVariantUsed(UUID variantId) {
        return orderDetailRepository.existsByShoeVariant_Id(variantId)
               || reviewRepository.existsByShoeVariant_Id(variantId);
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("error.pageable.required");
        }
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException("validation.page.min");
        }
        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 100) {
            throw new BadRequestException("error.page.size.invalid");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("error.price.range.invalid");
        }
    }

    private List<String> normalizeStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }

        List<String> normalized = statuses.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        for (String status : normalized) {
            if (ShoeStatus.fromId(status) == null) {
                throw new BadRequestException("error.shoe.status.invalid", "status", status);
            }
        }

        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> normalizeGenders(List<String> genders) {
        if (genders == null || genders.isEmpty()) {
            return null;
        }

        List<String> normalized = genders.stream()
                .filter(g -> g != null && !g.isBlank())
                .map(g -> g.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        for (String gender : normalized) {
            if (Gender.fromId(gender) == null) {
                throw new BadRequestException("error.shoe.gender.invalid", "gender", gender);
            }
        }

        return normalized.isEmpty() ? null : normalized;
    }

    private static String toVariantKey(String size, String color) {
        var normalizedSize = size == null ? "" : size.trim().toLowerCase(Locale.ROOT);
        var normalizedColor = color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
        return normalizedSize + "|" + normalizedColor;
    }

    private static final int MAX_UNIQUE_GENERATION_ATTEMPTS = 100;

    private String generateUniqueSlug(String baseSlug, UUID excludeId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            return "";
        }

        String candidate = baseSlug;
        int suffix = 0;

        while (suffix <= MAX_UNIQUE_GENERATION_ATTEMPTS) {
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

        log.error("Failed to generate a unique slug for {}", baseSlug);
        throw new InternalServerErrorException("error.shoe.slug.generate_failed", "slug", baseSlug);
    }

    private String generateUniqueSku(String baseSku) {
        String candidate = baseSku;
        int suffix = 0;

        while (suffix <= MAX_UNIQUE_GENERATION_ATTEMPTS) {
            if (!shoeVariantRepository.existsBySku(candidate)) {
                return candidate;
            }
            suffix++;
            candidate = SkuUtils.appendNumericSuffix(baseSku, suffix);
        }

        log.error("Failed to generate a unique SKU for {}", baseSku);
        throw new InternalServerErrorException("error.shoe.sku.generate_failed", "sku", baseSku);
    }

    private String generateUniqueSkuForUpdate(String baseSku, UUID variantId) {
        String candidate = baseSku;
        int suffix = 0;

        while (suffix <= MAX_UNIQUE_GENERATION_ATTEMPTS) {
            if (!shoeVariantRepository.existsBySkuAndIdNot(candidate, variantId)) {
                return candidate;
            }
            suffix++;
            candidate = SkuUtils.appendNumericSuffix(baseSku, suffix);
        }

        log.error("Failed to generate a unique SKU for {}", baseSku);
        throw new InternalServerErrorException("error.shoe.sku.generate_failed", "sku", baseSku);
    }
}
