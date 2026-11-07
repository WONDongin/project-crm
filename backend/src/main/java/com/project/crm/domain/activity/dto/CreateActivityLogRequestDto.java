package com.project.crm.domain.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 작성자, 생성 주체와 상담일은 서버에서 결정한다.
public record CreateActivityLogRequestDto(
        @NotBlank @Size(max = 20) String consultType,
        @NotBlank @Size(max = 20) String activityResult,
        @NotBlank @Size(max = 10000) String content,
        @Size(max = 255) String nextAction
) {}
