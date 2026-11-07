package com.project.crm.domain.activity;
// 상담 기록 비즈니스 로직

import org.springframework.stereotype.Service;

import com.project.crm.domain.customer.Customer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final com.project.crm.domain.user.UserRepository userRepository;

    // 상담 신청시  ACTIVITY_LOG 자동 생성
    public void createReceivedLog(
            Customer customer,
            String consultType,
            String preferredTime,
            String content
    ) {

        // 계정 변경과 FK 검사 사이의 잠금 역전을 피하도록 사용자 -> 고객 순서를 유지한다.
        if (customer.getCounselor() != null) {
            userRepository.findByIdForWork(customer.getCounselor().getUserId());
        }
        ActivityLog activityLog = ActivityLog.builder()
                .customer(customer)
                .counselor(customer.getCounselor())
                .createdBy("SYSTEM")
                .consultType(consultType)
                .activityResult("RECEIVED")
                .preferredTime(preferredTime)
                .content(content)
                .build();

        activityLogRepository.save(activityLog);
    }
}