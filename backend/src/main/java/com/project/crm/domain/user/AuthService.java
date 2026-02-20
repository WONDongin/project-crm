package com.project.crm.domain.user;

import com.project.crm.config.security.JwtUtil;
import com.project.crm.domain.user.dto.LoginRequest;
import com.project.crm.domain.user.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request){

        // 1.이메일 + ACTIVE 상태 조회
        User user = userRepository
                .findByEmailAndStatus(request.getEmail(), "ACTIVE")
                .orElseThrow(() ->
                        new IllegalArgumentException("로그인 실패")
                );

        // 2.비밀번호 검증
        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )){
            throw new IllegalArgumentException("로그인 실패");
        }

        // 3.마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 4.JWT 생성
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles(), // ex: ROLE_ADMIN
                user.getName()
        );

        return new LoginResponse(token, user.getRoles());
    }
}

