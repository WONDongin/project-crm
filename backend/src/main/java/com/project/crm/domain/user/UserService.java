package com.project.crm.domain.user;
// 사용자 관련 비즈니스 로직 (사용자 수정, 상태 변경, 비밀번호 초기화 처리 등)

import com.project.crm.domain.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 관리자 대시보드-사용자 관리
    public List<UserResponseDto> getUsers() {
        return  userRepository.findAll()
                .stream()
                .map(UserResponseDto::from)
                .toList();
    }
}
