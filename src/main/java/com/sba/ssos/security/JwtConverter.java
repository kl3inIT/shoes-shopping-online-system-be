package com.sba.ssos.security;

import com.sba.ssos.configuration.ApplicationProperties;
import com.sba.ssos.enums.UserRole;
import com.sba.ssos.exception.base.UnauthorizedException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

  static final String RESOURCE_ACCESS_CLAIM = "resource_access";
  static final String EMAIL_CLAIM = "email";
  private final LocaleUtils localeUtils;

  private final ApplicationProperties applicationProperties;

  @Override
  @SuppressWarnings("unchecked")
  public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
    var keycloakProperties = applicationProperties.keycloakProperties();
    var clientName = keycloakProperties.clientId();
    // cannot have different authorized party
    if (!clientName.equalsIgnoreCase(jwt.getClaimAsString("azp"))) {
      throw new UnauthorizedException(
          "error.jwt.invalid_azp", "expected", clientName, "actual", jwt.getClaimAsString("azp"));
    }

    // get the top-level "resource_access" claim.
    var resourceAccess =
        nonMissing(jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM), RESOURCE_ACCESS_CLAIM);

    // get the map specific to our client ID.
    var clientRolesMap =
        (Map<String, Collection<String>>)
            getMapValue(resourceAccess, clientName, RESOURCE_ACCESS_CLAIM);

    // get the collection of role strings from that map.
    var roleNames = getMapValue(clientRolesMap, "roles", RESOURCE_ACCESS_CLAIM, clientName);

    var validRoles = UserRole.fromRawRole(roleNames);

    Set<GrantedAuthority> authorities =
        validRoles.stream()
            .map(UserRole::name)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableSet());

    var userDetails =
        AuthorizedUserDetails.builder()
            .userId(UUID.fromString(nonMissing(jwt.getSubject(), "subject")))
            .username(nonMissing(jwt.getClaimAsString("preferred_username"), "username"))
            .email(nonMissing(jwt.getClaimAsString(EMAIL_CLAIM), EMAIL_CLAIM))
            .authorities(authorities)
            .build();

    return UsernamePasswordAuthenticationToken.authenticated(
        userDetails, jwt.getTokenValue(), authorities);
  }

  private static <T> T getMapValue(Map<String, T> map, String key, String... origins) {
    return nonMissing(
        map.get(key),
        ArrayUtils.isEmpty(origins) ? key : "%s.%s".formatted(String.join(".", origins), key));
  }

  private static <T> T nonMissing(T object, String name) {
    if (object == null) {
      throw new UnauthorizedException("error.jwt.claim_missing", "claim", name);
    }

    return object;
  }
}
