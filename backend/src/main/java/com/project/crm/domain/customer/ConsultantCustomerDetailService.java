package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLogRepository;
import com.project.crm.domain.customer.dto.ConsultantCustomerDetailResponseDto;
import lombok.RequiredArgsConstructor;
import com.project.crm.domain.activity.dto.ActivityLogPageResponseDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConsultantCustomerDetailService {

    private final CustomerRepository customerRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public ActivityLogPageResponseDto getHistory(Long customerId, Long counselorId, int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지는 0 이상이어야 합니다.");
        }
        customerRepository.findByCustomerIdAndCounselor_UserId(customerId, counselorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "담당 고객의 정보만 조회할 수 있습니다."
                ));
        var pageable = PageRequest.of(page, 5,
                Sort.by(Sort.Direction.DESC, "createdAt", "activityId"));
        return ActivityLogPageResponseDto.from(
                activityLogRepository.findByCustomer_CustomerId(customerId, pageable), counselorId);
    }

    @Transactional(readOnly = true)
    public ConsultantCustomerDetailResponseDto getDetail(Long customerId, Long counselorId) {
        // 존재 여부를 따로 공개하지 않고 담당 고객만 조회한다.
        Customer customer = customerRepository
                .findByCustomerIdAndCounselor_UserId(customerId, counselorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "담당 고객의 정보만 조회할 수 있습니다."
                ));

        var application = activityLogRepository
                .findFirstByCustomer_CustomerIdAndCreatedByAndActivityResultOrderByCreatedAtDescActivityIdDesc(
                        customerId, "SYSTEM", "RECEIVED"
                )
                .map(log -> new ConsultantCustomerDetailResponseDto.Application(
                        log.getActivityId(),
                        log.getPreferredTime(),
                        log.getContent(),
                        log.getCreatedAt()
                ))
                .orElse(null);

        return new ConsultantCustomerDetailResponseDto(
                customer.getCustomerId(),
                customer.getName(),
                customer.getPhone(),
                customer.getBirthDate(),
                customer.getEmail(),
                customer.getInterestCourse(),
                customer.getLeadSource(),
                customer.getStatus(),
                customer.getCustomerTag(),
                customer.getCounselor().getName(),
                customer.getLastActivityAt(),
                customer.getCreatedAt(),
                application
        );
    }
}

