package com.project.crm.domain.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 담당 고객의 현재 정보와 가장 최근 신청 접수 원본
public record ConsultantCustomerDetailResponseDto(
        Long customerId,
        String name,
        String phone,
        LocalDate birthDate,
        String email,
        String interestCourse,
        String leadSource,
        String status,
        String customerTag,
        String counselorName,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt,
        Application application
) {
    public record Application(
            Long activityId,
            String preferredTime,
            String content,
            LocalDateTime receivedAt
    ) {
    }
}
