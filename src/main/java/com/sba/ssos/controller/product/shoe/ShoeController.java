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
}
