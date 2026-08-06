package com.ht.eventbox.config;

import com.ht.eventbox.utils.KeyLoader;
import com.ht.eventbox.constant.Constant;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {
    @Value("${application.security.jwt.access-secret-key-path}")
    private Resource privateKeyResource;

    @Value("${application.security.jwt.access-public-key-path}")
    private Resource publicKeyResource;

    @Value("${application.security.public-apis-prefix}")
    private String[] publicApisPrefixConfig;

    @Value("${application.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${application.security.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${application.security.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${application.security.cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(
                    cors -> cors.configurationSource(corsConfigurationSource())
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers(publicApiMatchers()).permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
        ;

        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> {
            String headerToken = headerResolver.resolve(request);
            if (headerToken != null) {
                return headerToken;
            }

            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return null;
            }

            for (Cookie cookie : cookies) {
                if (Constant.ContextKey.ACCESS_TOKEN.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }

            return null;
        };
    }

    private Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (String permission : getStringClaim(jwt, "permissions")) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        for (String role : getStringClaim(jwt, "roles")) {
            String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(roleAuthority));
        }

        return authorities;
    }

    private List<String> getStringClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (!(claim instanceof Collection<?> values)) {
            return List.of();
        }

        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private String[] publicApiMatchers() {
        List<String> matchers = new ArrayList<>();

        for (String publicApiPrefix : publicApisPrefixConfig) {
            String matcher = publicApiPrefix.trim();
            if (matcher.isEmpty()) {
                continue;
            }

            if (matcher.endsWith("/")) {
                matchers.add(matcher + "**");
            } else {
                matchers.add(matcher);
                matchers.add(matcher + "/**");
            }
        }

        return matchers.toArray(String[]::new);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean(name = "atPrivateKey")
    public PrivateKey atPrivateKey() throws Exception {
        String key = StreamUtils.copyToString(privateKeyResource.getInputStream(), Charset.defaultCharset());
        return KeyLoader.loadPrivateKey(key);
    }

    @Bean(name = "atPublicKey")
    public PublicKey atPublicKey() throws Exception {
        String key = StreamUtils.copyToString(publicKeyResource.getInputStream(), Charset.defaultCharset());
        return KeyLoader.loadPublicKey(key);
    }
}
