package com.sba.ssos.controller;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.brand.BrandRequest;
import com.sba.ssos.dto.response.brand.BrandResponse;
import com.sba.ssos.service.BrandService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;
    private final LocaleUtils localeUtils;

    @GetMapping
    public ResponseGeneral<List<BrandResponse>> getAllBrands() {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.brand.fetched"),
                brandService.getAllBrands()
        );
    }

    @GetMapping("/{id}")
    public ResponseGeneral<BrandResponse> getBrandById(@PathVariable UUID id) {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.brand.retrieved"),
                brandService.getBrandById(id)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseGeneral<BrandResponse> createBrand(@RequestBody @Valid BrandRequest request) {
        return ResponseGeneral.ofCreated(
                localeUtils.get("success.brand.created"),
                brandService.createBrand(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseGeneral<BrandResponse> updateBrand(@PathVariable UUID id, @RequestBody @Valid BrandRequest request) {
        return ResponseGeneral.ofSuccess(
                localeUtils.get("success.brand.updated"),
                brandService.updateBrand(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseGeneral<Void> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.brand.deleted"));
    }
}
