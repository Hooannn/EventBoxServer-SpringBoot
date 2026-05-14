package com.ht.eventbox.filter;

import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.support.AbstractSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests extends AbstractSpringBootTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Qualifier("atPublicKey")
    private PublicKey atPublicKey;

    @Value("${application.security.jwt.refresh-secret-key}")
    private String refreshSecretKey;

    @Value("${application.security.jwt.qrcode-secret-key}")
    private String qrcodeSecretKey;

    @Test
    void generateAccessToken_shouldEmbedSubjectRolesAndPermissions() {
        User user = userWithRolesAndPermissions();

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractSub(token, atPublicKey)).isEqualTo("42");
        assertThat(jwtService.extractRoles(token, atPublicKey))
                .containsExactlyInAnyOrder("admin", "organizer");
        assertThat(jwtService.extractPermissions(token, atPublicKey))
                .containsExactlyInAnyOrder("create:events", "read:events");
        assertThat(jwtService.isTokenValid(token, atPublicKey)).isTrue();
    }

    @Test
    void generateRefreshToken_shouldBeValidWithRefreshSecret() {
        User user = userWithRolesAndPermissions();

        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractSub(token, refreshSecretKey)).isEqualTo("42");
        assertThat(jwtService.isTokenValid(token, refreshSecretKey)).isTrue();
    }

    @Test
    void generateQrCode_shouldBeValidWithQrSecret() {
        String token = jwtService.generateQrCode(99L);

        assertThat(jwtService.extractSub(token, qrcodeSecretKey)).isEqualTo("99");
        assertThat(jwtService.isTokenValid(token, qrcodeSecretKey)).isTrue();
    }

    private User userWithRolesAndPermissions() {
        Permission readEvents = Permission.builder()
                .name("read:events")
                .build();
        Permission createEvents = Permission.builder()
                .name("create:events")
                .build();

        Role admin = Role.builder()
                .name("admin")
                .permissions(new HashSet<>(Set.of(readEvents, createEvents)))
                .build();

        Role organizer = Role.builder()
                .name("organizer")
                .permissions(new HashSet<>(Set.of(readEvents)))
                .build();

        return User.builder()
                .id(42L)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .password("secret")
                .roles(new HashSet<>(Set.of(admin, organizer)))
                .build();
    }
}
