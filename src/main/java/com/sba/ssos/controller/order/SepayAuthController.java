package com.sba.ssos.controller.order;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.sepay.SepayTokenRequest;
import com.sba.ssos.dto.response.order.sepay.SepayTokenData;
import com.sba.ssos.service.sepay.SepayTokenService;
import com.sba.ssos.utils.LocaleUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.SEPAY_AUTH)
@Tag(name = "Sepay Auth", description = "Sepay authentication endpoints")
public class SepayAuthController {

  private final SepayTokenService tokenService;
  private final LocaleUtils localeUtils;

  @PostMapping("/token")
  public ResponseGeneral<SepayTokenData> getToken(@Valid @RequestBody SepayTokenRequest request) {
    return ResponseGeneral.ofSuccess(
        localeUtils.get("success.sepay.token.fetched"), tokenService.getKeyCloakToken(request));
  }
}
