package com.project.crm.api.admin;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.AdminCustomerAssignmentService;
import com.project.crm.domain.customer.CustomerBrowseService;
import com.project.crm.domain.customer.dto.AssignCounselorRequestDto;
import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
import com.project.crm.domain.user.dto.UserResponseDto;

import lombok.RequiredArgsConstructor;

// /api/admin/customers/** (고객 관리 - 고객 목록 조회, 고객 상태 변경, 담당자 변경 등)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerBrowseService browse;
    private final AdminCustomerAssignmentService assignmentService;

    // 전체 고객 목록 조회
    @GetMapping
    public ApiResponse<CustomerPageResponseDto> getCustomers(
            @RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="") String interestCourse,
            @RequestParam(required=false) Long counselorId,
            @RequestParam(defaultValue="false") boolean unassigned,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, Authentication authentication) {
        return ApiResponse.success(browse.search(authentication.getName(),true,keyword,status,interestCourse,counselorId,unassigned,page,size));
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
            @Valid @RequestBody AssignCounselorRequestDto request, Authentication authentication
    ) {
        assignmentService.assignCounselor(
                customerId,
                request.getCounselorId(), authentication.getName()
        );

        return ApiResponse.success(
                null,
                "담당 상담사가 저장되었습니다."
        );
    }

    @DeleteMapping("/{customerId}/counselor")
    public ApiResponse<Void> unassignCounselor(@PathVariable Long customerId, Authentication authentication) {
        assignmentService.unassignCounselor(customerId, authentication.getName());
        return ApiResponse.success(null, "담당자 배정이 해제되었습니다.");
    }
}
