package com.project.crm.config.security;

import com.project.crm.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {

            String email = jwtUtil.getEmail(token);
            Long id = jwtUtil.getUserId(token);
            Long version = jwtUtil.getAuthVersion(token);
            // DB 오류는 인증 실패로 숨기지 않는다. 유효한 서명이어도 기존 토큰/폐기 버전은 거부한다.
            var user = id == null ? null : users.findById(id).orElse(null);
            if (user != null && version != null && version == user.getAuthVersion()
                    && user.getEmail().equals(email) && "ACTIVE".equals(user.getStatus())
                    && user.getRoles().equals(jwtUtil.getRole(token))) {
                var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority(user.getRoles())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
