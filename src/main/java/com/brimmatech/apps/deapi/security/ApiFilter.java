package com.brimmatech.apps.deapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static com.brimmatech.apps.deapi.security.SecurityConfig.API_KEY_HEADER_NAME;
import static com.brimmatech.apps.deapi.security.SecurityConfig.TENANT_ID;

@Slf4j
public class ApiFilter extends OncePerRequestFilter {


    private AuthenticationManager authmanager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/docflow/")) {
            filterChain.doFilter(request, response);
            return;
        }

        val tenantId = Optional.ofNullable(request.getHeader(TENANT_ID))
                .map(Long::valueOf);
        var apiKey = Optional.ofNullable(request.getHeader(API_KEY_HEADER_NAME));

        if (Stream.of(tenantId, apiKey).allMatch(Optional::isEmpty)) {
            filterChain.doFilter(request, response);
            return;

        }

        if (Stream.of(tenantId, apiKey).anyMatch(Optional::isEmpty)) {
            throw new InsufficientAuthenticationException(
                    "Verify if the necessary authentication headers are present");

        }

        log.debug("Got headers as TENANT_ID:{}, API_KEY_HEADER_NAME{}", tenantId.get(), apiKey.get());
        var token = new ApiKeyAuthenticationToken(ApiKeyAuthenticationToken.Credentials.builder()
                .apiKey(apiKey.get())
                .tenantId(tenantId.get()).
                build());
        token.setDummyPrincipal(tenantId.get());

        SecurityContextHolder.getContext().setAuthentication(authmanager.authenticate(token));
        filterChain.doFilter(request, response);

    }

    public void setAuthenticationManager(AuthenticationManager authmanager) {
        this.authmanager = authmanager;
    }

//    @Override
//    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
//        super.successfulAuthentication(request, response, chain, authResult);
//        chain.doFilter(request, response);
//    }

}
