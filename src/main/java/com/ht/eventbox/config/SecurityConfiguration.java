package com.ht.eventbox.config;

import com.ht.eventbox.utils.KeyLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StreamUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {
    @Value("${application.security.jwt.access-secret-key-path}")
    private Resource privateKeyResource;

    @Value("${application.security.jwt.access-public-key-path}")
    private Resource publicKeyResource;

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
                        .requestMatchers("/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
        ;

        return http.build();
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
