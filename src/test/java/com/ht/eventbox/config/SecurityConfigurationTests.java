package com.ht.eventbox.config;

import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.filter.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigurationTests.TestController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfiguration.class,
        PermissionAuthorization.class,
        JwtService.class,
        SecurityConfigurationTests.TestController.class
})
class SecurityConfigurationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/secure"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withBearerToken_authenticates() throws Exception {
        mockMvc.perform(get("/secure")
                        .header("Authorization", "Bearer " + tokenWithPermissions("read:events")))
                .andExpect(status().isOk())
                .andExpect(content().string("secure"));
    }

    @Test
    void protectedEndpoint_withAccessTokenCookie_authenticates() throws Exception {
        mockMvc.perform(get("/secure")
                        .cookie(new Cookie(Constant.ContextKey.ACCESS_TOKEN, tokenWithPermissions("read:events"))))
                .andExpect(status().isOk())
                .andExpect(content().string("secure"));
    }

    @Test
    void permissionEndpoint_withMissingPermission_returnsForbidden() throws Exception {
        mockMvc.perform(get("/permission/read")
                        .header("Authorization", "Bearer " + tokenWithPermissions("create:events")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permissionEndpoint_withExactPermission_returnsOk() throws Exception {
        mockMvc.perform(get("/permission/read")
                        .header("Authorization", "Bearer " + tokenWithPermissions("read:events")))
                .andExpect(status().isOk())
                .andExpect(content().string("read"));
    }

    @Test
    void permissionEndpoint_withWildcardPermission_returnsOk() throws Exception {
        mockMvc.perform(get("/permission/read")
                        .header("Authorization", "Bearer " + tokenWithPermissions("read:*")))
                .andExpect(status().isOk())
                .andExpect(content().string("read"));
    }

    @Test
    void publicEndpoint_withoutToken_returnsOk() throws Exception {
        mockMvc.perform(get("/public/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void logoutEndpoint_withoutToken_isNotPermittedByAuthPrefix() throws Exception {
        mockMvc.perform(get("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    private String tokenWithPermissions(String... permissions) {
        Set<Permission> userPermissions = new HashSet<>();
        for (String permission : permissions) {
            userPermissions.add(Permission.builder()
                    .name(permission)
                    .build());
        }

        Role role = Role.builder()
                .name("user")
                .permissions(userPermissions)
                .build();

        User user = User.builder()
                .id(42L)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .password("password")
                .roles(new HashSet<>(Set.of(role)))
                .build();

        return jwtService.generateAccessToken(user);
    }

    @RestController
    static class TestController {
        @GetMapping("/secure")
        String secure() {
            return "secure";
        }

        @GetMapping("/permission/read")
        @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:events')")
        String read() {
            return "read";
        }

        @GetMapping("/public/api/v1/ping")
        String publicPing() {
            return "public";
        }

        @GetMapping("/api/v1/auth/logout")
        String logout() {
            return "logout";
        }
    }
}
