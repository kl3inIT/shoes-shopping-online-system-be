package com.sba.ssos.service.sepay;

import com.sba.ssos.dto.request.payment.sepay.SepayTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SepayTokenService {

    private static final String TOKEN_URL =
            "http://152.42.219.222:8080/realms/ssos/protocol/openid-connect/token";

    @Autowired
    private RestClient restClient;

    public String getKeyCloakToken(SepayTokenRequest sepayTokenRequest) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credential");
        requestBody.put("scope", "openid");

        try {
            return restClient.post()
                    .uri(TOKEN_URL)
                    .headers(httpHeaders -> {
                        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                        httpHeaders.setBasicAuth(
                                sepayTokenRequest.clientId(),
                                sepayTokenRequest.clientSecret()
                        );
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException ex) {
            throw new RuntimeException(
                    "Keycloak token request failed: " + ex.getResponseBodyAsString(),
                    ex
            );

        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error when calling Keycloak", ex);
        }
    }

}
