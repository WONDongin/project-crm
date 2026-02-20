package com.project.crm.api.auth;
// 로그인/회원가입/비밀번호 초기화 증 라우팅

import com.project.crm.domain.user.AuthService;
import com.project.crm.domain.user.User;
import com.project.crm.domain.user.UserRepository;
import com.project.crm.domain.user.dto.CustomPrincipal;
import com.project.crm.domain.user.dto.LoginRequest;
import com.project.crm.domain.user.dto.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // 로그인
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletResponse response){
        LoginResponse loginResponse = authService.login(request);

        // 쿠키 세팅 (minddleweare 대응)
        Cookie cookie = new Cookie("accessToken", loginResponse.getAccessToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60);
        // cookie.setSecure(true); // HTTPS 환경에서만 추후 운영때 활성화
        response.addCookie(cookie);

        return loginResponse;
    }

    // 로그아웃
    @PostMapping("/logout")
    public void logout(HttpServletResponse response){
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String role = authentication.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        return Map.of(
                "email", user.getEmail(),
                "name", user.getName(),
                "role", role
        );
    }
}
