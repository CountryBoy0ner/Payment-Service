package com.innowise.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String rolesHeader = request.getHeader("X-Roles");

        if (userId == null || username == null || rolesHeader == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        List<SimpleGrantedAuthority> authorities =
                Stream.of(rolesHeader.split(","))
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        CustomUser principal = new CustomUser(
                username,
                Long.parseLong(userId),
                authorities
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    public static class CustomUser {
        private final String username;
        private final Long userId;
        private final List<SimpleGrantedAuthority> authorities;

        public CustomUser(String username,
                          Long userId,
                          List<SimpleGrantedAuthority> authorities) {
            this.username = username;
            this.userId = userId;
            this.authorities = authorities;
        }

        public String getUsername() { return username; }
        public Long getUserId() { return userId; }
        public List<SimpleGrantedAuthority> getAuthorities() { return authorities; }
    }
}