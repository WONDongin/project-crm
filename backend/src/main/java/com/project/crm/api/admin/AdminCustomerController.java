package com.project.crm.api.admin;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.AdminCustomerAssignmentService;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.AssignCounselorRequestDto;
import com.project.crm.domain.customer.dto.CustomerResponseDto;
import com.project.crm.domain.user.dto.UserResponseDto;

import lombok.RequiredArgsConstructor;

// /api/admin/customers/** (고객 관리 - 고객 목록 조회, 고객 상태 변경, 담당자 변경 등)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final AdminCustomerAssignmentService assignmentService;

    // 전체 고객 목록 조회
    @GetMapping
    public ApiResponse<List<CustomerResponseDto>> getCustomers() {
        return ApiResponse.success(customerService.getCustomers());
    }

    // 배정 가능한 활성 상담사 목록 조회
    @GetMapping("/counselors")
    public ApiResponse<List<UserResponseDto>> getCounselors() {
        return ApiResponse.success(assignmentService.getCounselors());
    }

    // 고객 담당 상담사 배정 및 변경
    @PatchMapping("/{customerId}/counselor")
    public ApiResponse<Void> assignCounselor(
            @PathVariable Long customerId,
            @Valid @RequestBody AssignCounselorRequestDto request
    ) {
        assignmentService.assignCounselor(
                customerId,
                request.getCounselorId()
        );

        return ApiResponse.success(
                null,
                "담당 상담사가 저장되었습니다."
        );
    }
}