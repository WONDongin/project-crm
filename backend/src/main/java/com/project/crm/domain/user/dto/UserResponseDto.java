package com.project.crm.domain.user.dto;

import com.project.crm.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long userId;
    private String name;
    private String email;
    private String roles;
    private String specialty;
    private String status;
    private LocalDateTime lastLoginAt;

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getSpecialty(),
                user.getStatus(),
                user.getLastLoginAt()
        );
    }
}
