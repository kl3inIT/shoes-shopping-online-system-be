package com.sba.ssos.service.sepay;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.payment.sepay.SepayTokenRequest;
import com.sba.ssos.dto.response.payment.sepay.SepayTokenData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
@RequiredArgsConstructor
public class SepayTokenService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final ApplicationProperties applicationProperties;


    public SepayTokenData getKeyCloakToken(SepayTokenRequest sepayTokenRequest) {

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("scope", "openid");
        requestBody.add("username", applicationProperties.sepayProperties().sepayUserName());
        requestBody.add("password", applicationProperties.sepayProperties().sepayPassword());

        try {
            String token = restClient.post()
                    .uri(applicationProperties.keycloakProperties().tokenUrl())
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

            return sepayTokenData(token);

        } catch (RestClientResponseException ex) {
            throw new RuntimeException(
                    "Keycloak token request failed: " + ex.getResponseBodyAsString(),
                    ex
            );

        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error when calling Keycloak", ex);
        }
    }


    public SepayTokenData sepayTokenData(String token) {
        try {
            JsonNode root = objectMapper.readTree(token);

            return new SepayTokenData(
                    root.path("access_token").asText(),
                    root.path("refresh_token").asText(),
                    root.path("expires_in").asLong()
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot parse Keycloak token response", e
            );
        }

    }

}
