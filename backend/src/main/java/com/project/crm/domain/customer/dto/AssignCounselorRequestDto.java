package com.project.crm.domain.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 당당 상담사 배정요청
public class AssignCounselorRequestDto {

    @NotNull(message = "담당 상담사를 선택해 주세요.")
    @Positive(message = "올바른 상담사 번호를 입력해 주세요.")
    private Long counselorId;
}