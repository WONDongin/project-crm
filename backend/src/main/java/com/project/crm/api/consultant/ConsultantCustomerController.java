package com.project.crm.api.consultant;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.ConsultantCustomerResponseDto;
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

}
