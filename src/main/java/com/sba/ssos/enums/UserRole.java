package com.sba.ssos.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum UserRole {
    ROLE_ADMIN(Integer.MAX_VALUE),
    ROLE_MANAGER(Integer.MAX_VALUE - 1),
    ROLE_USER(0);

    // The higher the value, the more "superior" a role is
    private final int superiority;

    private static final Set<String> ROLE_LITERAL_SET =
            Arrays.stream(values())
                    .map(UserRole::name)
                    .collect(Collectors.toSet());

    public static Set<UserRole> fromRawRole(Collection<String> rawRoles) {
        return rawRoles.stream()
                .filter(UserRole::isValidRole)
                .map(String::toUpperCase)
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());
    }

    private static boolean isValidRole(String role) {
        return ROLE_LITERAL_SET.stream()
                .anyMatch(userRole -> userRole.equalsIgnoreCase(role));
    }
}
