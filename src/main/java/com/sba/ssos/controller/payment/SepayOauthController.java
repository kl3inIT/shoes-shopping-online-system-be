package com.sba.ssos.controller.payment;

import com.sba.ssos.dto.request.payment.sepay.SepayTokenRequest;
import com.sba.ssos.dto.response.payment.sepay.SepayTokenResponse;
import com.sba.ssos.service.sepay.SepayTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sepay/oauth")
public class SepayOauthController {

    private final SepayTokenService tokenService;

    @PostMapping(
            value = "/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SepayTokenResponse getToken(@RequestBody SepayTokenRequest request,
                                       CsrfToken csrfToken) {
        String token = tokenService.getKeyCloakToken(request);
        return  null;
    }




}
