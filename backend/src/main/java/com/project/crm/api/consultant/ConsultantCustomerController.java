package com.project.crm.api.consultant;

import lombok.RequiredArgsConstructor;
import com.project.crm.domain.activity.ConsultantActivityLogService;
import com.project.crm.domain.activity.dto.CreateActivityLogRequestDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import com.project.crm.domain.activity.dto.ActivityLogPageResponseDto;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.ConsultantCustomerDetailService;
import com.project.crm.domain.customer.dto.ConsultantCustomerDetailResponseDto;
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
    private final ConsultantActivityLogService activityLogService;
    private final com.project.crm.domain.customer.CustomerProfileService profileService;
    private final CustomerService customerService;
    private final ConsultantCustomerDetailService customerDetailService;
    private final UserRepository userRepository;

    @PostMapping("/{customerId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createActivity(
            @PathVariable Long customerId,
            @Valid @RequestBody CreateActivityLogRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.success(activityLogService.create(customerId, authentication.getName(), request),
                "상담 기록이 등록되었습니다.");
    }

    @PatchMapping("/{customerId}/activities/{activityId}")
    public ApiResponse<Void> updateActivity(
            @PathVariable Long customerId,
            @PathVariable Long activityId,
            @Valid @RequestBody CreateActivityLogRequestDto request,
            Authentication authentication
    ) {
        activityLogService.update(customerId, activityId, authentication.getName(), request);
        return ApiResponse.success(null, "상담 기록이 수정되었습니다.");
    }

    @PatchMapping("/{customerId}/profile")
    public ApiResponse<Void> updateProfile(@PathVariable Long customerId,
            @Valid @RequestBody com.project.crm.domain.customer.dto.UpdateCustomerProfileRequestDto request,
            Authentication authentication) {
        try {
            profileService.update(customerId, authentication.getName(), request);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "중복 고객 정보 또는 입력값을 확인해 주세요.");
        }
        return ApiResponse.success(null, "고객 정보가 수정되었습니다.");
    }

    // 신청 접수 원본과 상담 이력을 최신순으로 5건씩 조회
    @GetMapping("/{customerId}/activities")
    public ApiResponse<ActivityLogPageResponseDto> getCustomerHistory(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication
    ) {
        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다."
                ));
        return ApiResponse.success(customerDetailService.getHistory(customerId, user.getUserId(), page));
    }

    // 담당 고객 상세 및 가장 최근 상담 신청 정보 조회
    @GetMapping("/{customerId}")
    public ApiResponse<ConsultantCustomerDetailResponseDto> getCustomerDetail(
            @PathVariable Long customerId,
            Authentication authentication
    ) {
        String email = (String) authentication.getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "로그인 정보를 확인할 수 없습니다."
                ));

        return ApiResponse.success(customerDetailService.getDetail(customerId, user.getUserId()));
    }

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


