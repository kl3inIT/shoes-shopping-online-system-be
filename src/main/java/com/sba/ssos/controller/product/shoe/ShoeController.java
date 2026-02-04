package com.sba.ssos.controller.product.shoe;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.service.product.shoe.ShoeService;
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
    private final LocaleUtils localeUtils;

    @PostMapping
    public ResponseGeneral<ShoeResponse> create(@Valid @RequestBody ShoeCreateRequest request) {
        ShoeResponse data = shoeService.create(request);
        return ResponseGeneral.ofCreated(localeUtils.get("success.shoe.created"), data);
    }

    @GetMapping
    public ResponseGeneral<List<ShoeResponse>> getAll() {
        List<ShoeResponse> data = shoeService.getAll();
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }

    @GetMapping("/{id}")
    public ResponseGeneral<ShoeResponse> getById(@PathVariable UUID id) {
        ShoeResponse data = shoeService.getById(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.shoe.fetched"), data);
    }
}
