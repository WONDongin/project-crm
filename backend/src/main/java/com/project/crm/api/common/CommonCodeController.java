package com.project.crm.api.common;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.code.CodeService;
import com.project.crm.domain.code.dto.CodeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/common-codes")
public class CommonCodeController {

    private final CodeService codeService;

    @GetMapping
    public ApiResponse<Map<String, List<CodeResponseDto>>> getCodes() {

        return ApiResponse.success(
                codeService.getGroupedCodes()
        );
    }

}