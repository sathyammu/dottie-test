package com.brimmatech.apps.deapi.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    public static String API_KEY_HEADER_NAME = "X-API-KEY";
    public static final String TENANT_ID = "X-TENANT-ID";

    private final ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;
    private final AuthEntryPoint authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {

        http
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, excep) -> res.sendError(HttpStatus.FORBIDDEN.value()))
                )
                .securityMatcher("/admin/**", "/ui/**", "/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll())
                .sessionManagement(management -> {
                    management.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
                })
                .anonymous(a -> a.principal("dfUser") // Set a custom principal for anonymous users
                        .authorities("ROLE_DF_USER"))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiHeaderFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(apiKeyAuthenticationProvider);

        ApiFilter filter = new ApiFilter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/v3/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterAfter(filter, LogoutFilter.class);

        val chain = http.build();

        AuthenticationManager authManager = http.getSharedObject(AuthenticationManager.class);
        filter.setAuthenticationManager(authManager);

        return chain;
    }


}
