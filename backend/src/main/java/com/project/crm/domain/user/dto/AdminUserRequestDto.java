package com.project.crm.domain.user.dto;

import jakarta.validation.constraints.*;

public final class AdminUserRequestDto {
    private AdminUserRequestDto() {}
    public record Create(
            @NotBlank @Email @Size(max=100) String email,
            @NotBlank @Size(max=50) String name,
            @NotBlank @Size(min=8,max=20) String password,
            @NotBlank @Size(max=20) String roles,
            @NotBlank @Size(max=20) String specialty,
            @NotBlank @Size(max=20) String status,
            @Size(max=10000) String memo) {}
    public record Update(
            @NotBlank @Email @Size(max=100) String email,
            @NotBlank @Size(max=50) String name,
            @NotBlank @Size(max=20) String roles,
            @NotBlank @Size(max=20) String specialty,
            @NotBlank @Size(max=20) String status,
            @Size(max=10000) String memo) {}
    public record Password(@NotBlank @Size(min=8,max=20) String password) {}
    public record Change(UserResponseDto user, long releasedCustomerCount, boolean reauthenticationRequired) {}
}
