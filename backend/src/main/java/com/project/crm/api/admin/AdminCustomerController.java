package com.project.crm.api.admin;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.CustomerResponseDto;

import lombok.RequiredArgsConstructor;

// /api/admin/customers/** (고객 관리 - 고객 목록 조회, 고객 상태 변경, 담당자 변경 등)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/customers")
public class AdminCustomerController {
    
    private final CustomerService customerService;

    @GetExchange
    public ApiResponse<List<CustomerResponseDto>> getCustomers() {
        
        return  ApiResponse.success(customerService.getCustomers());
    }
}
