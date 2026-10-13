package com.project.crm.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerStatusRequestDto {

    @NotBlank(message = "변경할 고객 상태를 선택하세요.")
    private String status;
}
