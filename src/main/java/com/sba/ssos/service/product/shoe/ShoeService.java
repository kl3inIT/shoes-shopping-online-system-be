package com.sba.ssos.service.product.shoe;

import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeUpdateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeStatusUpdateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeUpdateRequest;
import com.sba.ssos.dto.request.product.shoevariant.ShoeVariantRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.entity.Category;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.enums.Gender;
import com.sba.ssos.enums.ShoeStatus;
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
import com.sba.ssos.service.product.shoeimage.ShoeImageService;
import com.sba.ssos.utils.SkuUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
                shoe.getLastUpdatedAt()
        );
    }

    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        return toShoeResponse(shoe);
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
        Map<UUID, ShoeVariant> existingVariantById = new HashMap<>();
        Map<String, ShoeVariant> existingVariantByKey = new HashMap<>();
        for (ShoeVariant v : existingVariants) {
            existingVariantById.put(v.getId(), v);
            existingVariantByKey.put(toVariantKey(v.getSize(), v.getColor()), v);
        }

        Set<String> seenKeys = new HashSet<>();
        for (ShoeVariantRequest v : request.variants()) {
            var key = toVariantKey(v.size(), v.color());
            if (!seenKeys.add(key)) {
                throw new ConflictException(
                        "error.shoe.variant.duplicate",
                        "shoeId", id,
                        "size", v.size(),
                        "color", v.color()
                );
            }
        }

        Set<UUID> touchedVariantIds = new HashSet<>();
        List<ShoeVariant> variantsInRequestOrder = new ArrayList<>();

        for (ShoeVariantRequest variantRequest : request.variants()) {
            var requestedId = variantRequest.id();
            var requestedSize = variantRequest.size().trim();
            var requestedColor = variantRequest.color().trim();
            var requestedQuantity = variantRequest.quantity();
            var requestedActive = variantRequest.active() == null || variantRequest.active();
            var requestedKey = toVariantKey(requestedSize, requestedColor);

            ShoeVariant variant;

            if (requestedId != null) {
                variant = existingVariantById.get(requestedId);
                if (variant == null) {
                    throw new NotFoundException("ShoeVariant", requestedId);
                }
            } else {
                variant = existingVariantByKey.get(requestedKey);
            }

            if (variant == null) {
                String baseSku = SkuUtils.buildBaseSku(shoe.getSlug(), requestedSize, requestedColor);
                String uniqueSku = generateUniqueSku(baseSku);

                variant = ShoeVariant.builder()
                        .shoe(shoe)
                        .size(requestedSize)
                        .color(requestedColor)
                        .quantity(requestedQuantity)
                        .sku(uniqueSku)
                        .active(requestedActive)
                        .build();

                variant = shoeVariantRepository.save(variant);
            } else {
                boolean usedInOrder = orderDetailRepository.existsByShoeVariant_Id(variant.getId());
                boolean usedInReview = reviewRepository.existsByShoeVariant_Id(variant.getId());
                boolean isUsed = usedInOrder || usedInReview;

                if (isUsed) {
                    boolean sizeChanged = !variant.getSize().equalsIgnoreCase(requestedSize);
                    boolean colorChanged = !variant.getColor().equalsIgnoreCase(requestedColor);
                    if (sizeChanged || colorChanged) {
                        throw new ConflictException(
                                "error.shoe.variant.cannot.modify.used",
                                "shoeId", id,
                                "variantId", variant.getId()
                        );
                    }
                } else {
                    var conflict = existingVariantByKey.get(requestedKey);
                    if (conflict != null && !conflict.getId().equals(variant.getId())) {
                        throw new ConflictException(
                                "error.shoe.variant.duplicate",
                                "shoeId", id,
                                "size", requestedSize,
                                "color", requestedColor
                        );
                    }

                    boolean sizeChanged = !variant.getSize().equalsIgnoreCase(requestedSize);
                    boolean colorChanged = !variant.getColor().equalsIgnoreCase(requestedColor);
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
                variant = shoeVariantRepository.save(variant);
            }

            touchedVariantIds.add(variant.getId());
            variantsInRequestOrder.add(variant);
        }

        for (ShoeVariant existingVariant : existingVariants) {
            if (touchedVariantIds.contains(existingVariant.getId())) {
                continue;
            }

            boolean usedInOrder = orderDetailRepository.existsByShoeVariant_Id(existingVariant.getId());
            boolean usedInReview = reviewRepository.existsByShoeVariant_Id(existingVariant.getId());
            boolean isUsed = usedInOrder || usedInReview;

            if (isUsed) {
                existingVariant.setActive(false);
                existingVariant.setQuantity(0L);
                shoeVariantRepository.save(existingVariant);
                continue;
            }

            cartItemRepository.deleteAllByShoeVariant_Id(existingVariant.getId());
            shoeImageRepository.deleteAllByShoeVariant_Id(existingVariant.getId());
            shoeVariantRepository.delete(existingVariant);
        }

        if (shoeImageFiles != null && !shoeImageFiles.isEmpty()) {
            shoeImageService.uploadShoeImages(shoe, variantsInRequestOrder, shoeImageFiles);
        }
        if (variantImageFilesList != null && !variantImageFilesList.isEmpty()) {
            shoeImageService.uploadVariantImages(shoe, variantsInRequestOrder, variantImageFilesList);
        }

        return getById(id);
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
        validatePageable(pageable);
        validatePriceRange(minPrice, maxPrice);

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

        return page.map(this::toShoeResponse);
    }

    public List<ShoeResponse> getNewArrivals(int limit) {
        List<Shoe> shoes = shoeRepository.findAll(PageRequest.of(0, limit)).getContent();
        return shoes.stream().map(this::toShoeResponse).toList();
    }

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
        return shoes.stream().map(this::toShoeResponse).toList();
    }

    private ShoeResponse toShoeResponse(Shoe shoe) {
        List<ShoeVariant> variants =
                shoeVariantRepository.findByShoe_IdAndActiveTrueOrderBySizeAscColorAsc(
                        shoe.getId());
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
                shoe.getLastUpdatedAt()
        );
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("pageable must not be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must be less than or equal to maxPrice");
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
                throw new IllegalArgumentException("Invalid shoe status: " + status);
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
                throw new IllegalArgumentException("Invalid gender: " + gender);
            }
        }

        return normalized.isEmpty() ? null : normalized;
    }

    private static String toVariantKey(String size, String color) {
        var normalizedSize = size == null ? "" : size.trim().toLowerCase(Locale.ROOT);
        var normalizedColor = color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
        return normalizedSize + "|" + normalizedColor;
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

    private String generateUniqueSkuForUpdate(String baseSku, UUID variantId) {
        String candidate = baseSku;
        int suffix = 0;

        while (shoeVariantRepository.existsBySkuAndIdNot(candidate, variantId)) {
            suffix++;
            candidate = SkuUtils.appendNumericSuffix(baseSku, suffix);
        }

        return candidate;
    }
}
