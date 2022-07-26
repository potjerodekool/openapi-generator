package io.github.potjerodekool.demo.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final var authorizationStr = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationStr != null && authorizationStr.startsWith(BEARER_PREFIX)) {
            final var bearerToken = authorizationStr.substring(BEARER_PREFIX.length());

            if (bearerToken.length() > 0) {
                final var context = SecurityContextHolder.getContext();
                context.setAuthentication(new JwtUser(bearerToken));
            }
        }

        filterChain.doFilter(request, response);
    }
}
