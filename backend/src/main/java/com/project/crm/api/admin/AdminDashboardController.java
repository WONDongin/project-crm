package com.project.crm.api.admin;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService dashboard;

    @GetMapping
    public ApiResponse<AdminDashboardService.Dashboard> get(Authentication authentication) {
        return ApiResponse.success(dashboard.get(authentication.getName()));
    }
}
