package com.sba.ssos.service.sepay;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.dto.request.order.sepay.SepayTokenRequest;
import com.sba.ssos.dto.response.order.sepay.SepayTokenData;
import com.sba.ssos.exception.base.InternalServerErrorException;
import com.sba.ssos.exception.base.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.info("Requesting SePay Keycloak token");
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
            if (ex.getStatusCode().is4xxClientError()) {
                log.warn("SePay Keycloak token request failed with status {}", ex.getStatusCode().value());
                throw new UnauthorizedException("error.sepay.auth.failed");
            }
            log.error("SePay Keycloak token request failed with status {}", ex.getStatusCode().value(), ex);
            throw new InternalServerErrorException("error.sepay.token.request_failed", ex);

        } catch (Exception ex) {
            log.error("Unexpected error while requesting SePay Keycloak token", ex);
            throw new InternalServerErrorException("error.sepay.token.request_failed", ex);
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
            log.error("Failed to parse SePay Keycloak token response", e);
            throw new InternalServerErrorException("error.sepay.token.parse_failed", e);
        }

    }

}
