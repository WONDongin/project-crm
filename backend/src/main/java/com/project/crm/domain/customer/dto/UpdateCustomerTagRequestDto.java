package com.project.crm.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerTagRequestDto {

    @NotBlank(message = "변경할 고객 태그를 선택하세요.")
    private String customerTag;
}
