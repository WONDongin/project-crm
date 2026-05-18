package com.project.crm.domain.customer.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerResponseDto {

    private Long customerId;

    private String name;

    private String phone;

    private String interestCourse;

    private String status;

    private Long counselorId;

    private String counselorName;
}