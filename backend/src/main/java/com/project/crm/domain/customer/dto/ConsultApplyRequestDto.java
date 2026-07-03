package com.project.crm.domain.customer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
// 상담 신청 요청 DTO
public class ConsultApplyRequestDto {

    private String name;

    private LocalDate birthDate;

    private String phone;

    private String email;

    private String interestCourse;

    private String leadSource;

    private String content;
}