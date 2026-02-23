package com.sba.ssos.controller.product.shoe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.dto.response.product.shoevariant.ShoeVariantResponse;
import com.sba.ssos.service.product.shoe.ShoeService;
import com.sba.ssos.service.product.shoevariant.ShoeVariantService;
import com.sba.ssos.utils.LocaleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseGeneral<ShoeResponse> create(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "shoeImages", required = false) List<MultipartFile> shoeImages,
            MultipartHttpServletRequest multiRequest
    ) {
        try {
            ShoeCreateRequest request = objectMapper.readValue(requestJson, ShoeCreateRequest.class);
            
            List<List<MultipartFile>> variantImagesList = new ArrayList<>();
            int variantCount = request.variants().size();
            
            for (int i = 0; i < variantCount; i++) {
                List<MultipartFile> files = multiRequest.getFiles("variantImages" + i);
                variantImagesList.add(files != null ? files : new ArrayList<>());
            }
            
            ShoeResponse data = shoeService.create(request, shoeImages, variantImagesList);
            return ResponseGeneral.ofCreated(localeUtils.get("success.shoe.created"), data);
        } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
            throw new RuntimeException("Invalid request format", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process request: " + e.getMessage(), e);
        }
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

    @GetMapping("/{id}/variants")
    public ResponseGeneral<List<ShoeVariantResponse>> getVariantsByShoeId(@PathVariable UUID id) {
        List<ShoeVariantResponse> data = shoeVariantService.getVariantsByShoeId(id);
        return ResponseGeneral.ofSuccess(localeUtils.get("success.generic"), data);
    }
}
