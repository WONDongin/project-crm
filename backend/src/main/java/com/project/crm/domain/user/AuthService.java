package com.project.crm.domain.user;

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

    public LoginResponse login(LoginRequest request){

        // 이메일 + ACTIVE 상태 조회
        User user = userRepository
                .findByEmailAndStatus(request.getEmail(), "ACTIVE")
                .orElseThrow(() ->
                    new IllegalArgumentException("로그인 실패")
                );

        // 비밀번호 검증
        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )){
            throw new IllegalArgumentException("로그인 실패");
        }

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 임시토큰
        String dummyToken = "test-token";

        return new LoginResponse(dummyToken, user.getRoles());
    }
}
