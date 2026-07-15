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

    // 상담 신청시  ACTIVITY_LOG 자동 생성
    public void createReceivedLog(
            Customer customer,
            String preferredTime,
            String content
    ) {

        ActivityLog activityLog = ActivityLog.builder()
                .customer(customer)
                .createdBy("SYSTEM")
                .activityResult("RECEIVED")
                .preferredTime(preferredTime)
                .content(content)
                .build();

        activityLogRepository.save(activityLog);
    }
}
