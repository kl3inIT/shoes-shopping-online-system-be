package com.sba.ssos.controller.product.shoe;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.service.product.shoe.ShoeService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/shoes")
public class ShoeController {

    private final ShoeService shoeService;
    private final ShoeVariantService shoeVariantService;
    private final LocaleUtils localeUtils;

    @GetMapping("/{id}/variants")
    public ResponseGeneral<List<ShoeVariantResponse>> getVariantsByShoeId(@PathVariable UUID id) {
        List<ShoeVariantResponse> data = shoeVariantService.getVariantsByShoeId(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.generic"), data);
    }

    @PostMapping
    public ResponseGeneral<ShoeResponse> create(@Valid @RequestBody ShoeCreateRequest request) {
        ShoeResponse data = shoeService.create(request);
        return ResponseGeneral.ofCreated(localeUtils.get("success.shoe.created"), data);
    }
}
