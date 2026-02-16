package com.project.crm.api.auth;
// 로그인/회원가입/비밀번호 초기화 증 라우팅

import com.project.crm.domain.user.AuthService;
import com.project.crm.domain.user.dto.LoginRequest;
import com.project.crm.domain.user.dto.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletResponse response){
        LoginResponse loginResponse = authService.login(request);

        // 쿠키 세팅 (minddleweare 대응)
        Cookie cookie = new Cookie("accessToken", loginResponse.getAccessToken());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return loginResponse;
    }
}
