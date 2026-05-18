package com.project.crm.domain.customer.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor

// 상담사별 고객 조회 응답 DTO
public class ConsultantCustomerResponseDto {
    private Long customerId;

    private String name;

    private String phone;

    private String interestCourse;

    private String status;

    private String customerTag;

    private Long counselorId;

    private String counselorName;

    private LocalDateTime lastActivityAt;

    private String leadSource;
}
