package com.project.crm.api.consultant;

import lombok.RequiredArgsConstructor;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.ConsultantCustomerResponseDto;
import com.project.crm.domain.customer.dto.UpdateConsultantCustomerRequestDto;
import com.project.crm.domain.customer.dto.UpdateCustomerTagRequestDto;
import com.project.crm.domain.customer.dto.UpdateCustomerStatusRequestDto;
import com.project.crm.domain.user.User;
import com.project.crm.domain.user.UserRepository;



// 상담사 고객 목록/상세/상태 변경/상담 기록 등 라우팅
@RestController
@RequestMapping("/consultant/customers")
@RequiredArgsConstructor
public class ConsultantCustomerController {
    private final CustomerService customerService;
    private final UserRepository userRepository;

    // 상담사별 고객 조회
    @GetMapping
    public ApiResponse<List<ConsultantCustomerResponseDto>> getCustomers(
            Authentication authentication
    ) {

        // JWT principal = email
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ApiResponse.success(
                customerService.getConsultantCustomers(user.getUserId())
        );

    }

    // 담당 고객 상태 변경
    @PatchMapping("/{customerId}/status")
    public ApiResponse<Void> updateCustomerStatus(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerStatusRequestDto request,
            Authentication authentication
    ) {
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        customerService.updateConsultantCustomerStatus(
                customerId,
                user.getUserId(),
                request.getStatus()
        );

        return ApiResponse.success(null, "상태가 저장되었습니다");
    }

    // 담당 고객 태그 변경
    @PatchMapping("/{customerId}/tag")
    public ApiResponse<Void> updateCustomerTag(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerTagRequestDto request,
            Authentication authentication
    ) {
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        customerService.updateConsultantCustomerTag(
                customerId,
                user.getUserId(),
                request.getCustomerTag()
        );

        return ApiResponse.success(null, "고객 태그가 저장되었습니다");
    }

    // 담당 고객 상태 및 태그 변경
    @PatchMapping("/{customerId}")
    public ApiResponse<Void> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody UpdateConsultantCustomerRequestDto request,
            Authentication authentication
    ) {
        String email = (String) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        customerService.updateConsultantCustomer(
                customerId,
                user.getUserId(),
                request
        );

        return ApiResponse.success(null, "고객 정보가 저장되었습니다");
    }

}
