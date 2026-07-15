package com.project.crm.domain.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
// 상담 신청 요청 DTO
public class ConsultApplyRequestDto {

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    @NotNull(message = "생년월일을 입력하세요.")
    private LocalDate birthDate;

    @NotBlank(message = "연락처를 입력하세요.")
    private String phone;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "관심 과정을 선택하세요.")
    private String interestCourse;

    @NotBlank(message = "상담 희망 시간을 선택하세요.")
    private String preferredTime;

    @NotBlank(message = "유입 경로를 선택하세요.")
    private String leadSource;

    @NotBlank(message = "상담 희망 내용을 입력하세요.")
    private String content;
}