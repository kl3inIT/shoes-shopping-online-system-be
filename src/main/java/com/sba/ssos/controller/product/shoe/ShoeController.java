package com.sba.ssos.controller.product.shoe;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.request.product.shoe.ShoeUpdateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.service.product.shoe.ShoeService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/shoes")
public class ShoeController {

    private final ShoeService shoeService;
    private final ShoeVariantService shoeVariantService;
    private final LocaleUtils localeUtils;

    // ===== Admin =====

    @GetMapping("/admin/all")
    public ResponseGeneral<List<ShoeResponse>> getAdminAll() {
        List<ShoeResponse> data = shoeService.getAdminAll();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/admin/deleted")
    public ResponseGeneral<List<ShoeResponse>> getAdminDeleted() {
        List<ShoeResponse> data = shoeService.getAdminDeleted();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/admin/not-deleted")
    public ResponseGeneral<List<ShoeResponse>> getAdminNotDeleted() {
        List<ShoeResponse> data = shoeService.getAdminNotDeleted();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseGeneral<ShoeResponse> create(
            @Valid @RequestPart("request") ShoeCreateRequest request,
            @RequestPart(value = "shoeImages", required = false) List<MultipartFile> shoeImages,
            MultipartHttpServletRequest multiRequest
    ) {
        List<List<MultipartFile>> variantImagesList = new ArrayList<>();
        int variantCount = request.variants().size();

        for (int i = 0; i < variantCount; i++) {
            List<MultipartFile> files = multiRequest.getFiles("variantImages" + i);
            variantImagesList.add(files != null ? files : new ArrayList<>());
        }

        ShoeResponse data = shoeService.create(request, shoeImages, variantImagesList);
        return ResponseGeneral.ofCreated(localeUtils.get("success.shoe.created"), data);
    }

    @GetMapping
    public ResponseGeneral<Page<ShoeResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) List<String> sizes,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> genders,
            @PageableDefault(sort = "createdAt") Pageable pageable
    ) {
        Page<ShoeResponse> data = shoeService.search(
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
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/best-sellers")
    public ResponseGeneral<List<ShoeResponse>> getBestSellers(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<ShoeResponse> data = shoeService.getBestSellers(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/new-arrivals")
    public ResponseGeneral<List<ShoeResponse>> getNewArrivals(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<ShoeResponse> data = shoeService.getNewArrivals(limit);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/{id}")
    public ResponseGeneral<ShoeResponse> getById(@PathVariable UUID id) {
        ShoeResponse data = shoeService.getById(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/{id}/variants")
    public ResponseGeneral<List<ShoeVariantResponse>> getVariantsByShoeId(@PathVariable UUID id) {
        List<ShoeVariantResponse> data = shoeVariantService.getVariantsByShoeId(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.generic"), data);
    }

    @DeleteMapping("/{id}")
    public ResponseGeneral<Void> delete(@PathVariable UUID id) {
        shoeService.softDelete(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.updated"));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseGeneral<ShoeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestPart("request") ShoeUpdateRequest request,
            @RequestPart(value = "shoeImages", required = false) List<MultipartFile> shoeImages,
            MultipartHttpServletRequest multiRequest
    ) {
        List<List<MultipartFile>> variantImagesList = new ArrayList<>();
        int variantCount = request.variants().size();

        for (int i = 0; i < variantCount; i++) {
            List<MultipartFile> files = multiRequest.getFiles("variantImages" + i);
            variantImagesList.add(files != null ? files : new ArrayList<>());
        }

        ShoeResponse data = shoeService.update(id, request, shoeImages, variantImagesList);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.updated"), data);
    }
}
