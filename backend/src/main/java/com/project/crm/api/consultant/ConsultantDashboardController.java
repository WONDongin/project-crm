package com.project.crm.api.consultant;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.ConsultantDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/consultant/dashboard")
public class ConsultantDashboardController {
    private final ConsultantDashboardService dashboard;

    @GetMapping
    public ApiResponse<ConsultantDashboardService.Dashboard> get(Authentication authentication) {
        return ApiResponse.success(dashboard.get(authentication.getName()));
    }
}
