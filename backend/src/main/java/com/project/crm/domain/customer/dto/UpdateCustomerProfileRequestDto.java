package com.project.crm.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequestDto(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 20) @Pattern(regexp = "[0-9() .+-]{9,20}") String phone,
        @NotBlank @Size(max = 30) String interestCourse
) {}
