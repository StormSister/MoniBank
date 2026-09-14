package com.monibank.mainframe.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Instant;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUserDetailsService(
            AdminSecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        return new InMemoryUserDetailsManager(
                User.withUsername(properties.username())
                        .password(passwordEncoder.encode(properties.password()))
                        .authorities("ROLE_ADMIN")
                        .build()
        );
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtEncoder jwtEncoder(AdminSecurityProperties properties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(properties))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(AdminSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        AdminTokenService.ISSUER
                )
        );
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiRateLimitFilter rateLimitFilter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/token"
                        ).permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasAuthority("SCOPE_admin")
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint((request, response, error) ->
                                writeSecurityError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "AUTHENTICATION_REQUIRED",
                                        "A valid administrator token is required."
                                )
                        )
                        .accessDeniedHandler((request, response, error) ->
                                writeSecurityError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED",
                                        "Administrator access is required."
                                )
                        )
                )
                .addFilterAfter(
                        rateLimitFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    FilterRegistrationBean<ApiRateLimitFilter> disableContainerRegistration(
            ApiRateLimitFilter filter
    ) {
        FilterRegistrationBean<ApiRateLimitFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static SecretKey secretKey(
            AdminSecurityProperties properties
    ) {
        return new SecretKeySpec(
                properties.decodedJwtSecret(),
                "HmacSHA256"
        );
    }

    private static void writeSecurityError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now()
                        + "\",\"status\":" + status
                        + ",\"code\":\"" + code
                        + "\",\"message\":\"" + message
                        + "\",\"retryable\":false}"
        );
    }
}
