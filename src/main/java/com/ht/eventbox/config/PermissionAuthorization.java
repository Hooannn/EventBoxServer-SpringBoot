package com.ht.eventbox.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component("permissionAuthorization")
public class PermissionAuthorization {
    public boolean hasAll(Authentication authentication, String... requiredPermissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String requiredPermission : requiredPermissions) {
            if (!hasPermission(authorities, requiredPermission)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasPermission(Set<String> authorities, String requiredPermission) {
        if (authorities.contains(requiredPermission)) {
            return true;
        }

        String[] requiredParts = requiredPermission.split(":");
        if (requiredParts.length == 0) {
            return false;
        }

        return authorities.contains(requiredParts[0] + ":*");
    }
}
