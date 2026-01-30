package com.sba.ssos.controller.order;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.order.sepay.SepayTokenRequest;
import com.sba.ssos.dto.response.payment.sepay.SepayTokenData;
import com.sba.ssos.service.sepay.SepayTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sepay/auth")
public class SepayOauthController {

    private final SepayTokenService tokenService;

    @PostMapping(value = "/token")
    public ResponseGeneral<SepayTokenData> getToken(@RequestBody SepayTokenRequest request) {
        SepayTokenData token = tokenService.getKeyCloakToken(request);
        return ResponseGeneral.ofSuccess("success", token);
    }


}
