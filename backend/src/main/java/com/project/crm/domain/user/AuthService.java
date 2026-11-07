package com.project.crm.domain.user;

import com.project.crm.config.security.JwtUtil;
import com.project.crm.domain.user.dto.LoginRequest;
import com.project.crm.domain.user.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse login(LoginRequest request){

        // 1.이메일 + ACTIVE 상태 조회
        User user = userRepository
                .findByEmailAndStatus(request.getEmail(), "ACTIVE")
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인해 주세요.")
                );

        // 2.비밀번호 검증
        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인해 주세요.");
        }

        // 3.마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 4.JWT 생성
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRoles(), // ex: ROLE_ADMIN
                user.getName(), user.getUserId(), user.getAuthVersion()
        );

        return new LoginResponse(token, user.getRoles());
    }
}

