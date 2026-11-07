package com.project.crm.domain.activity;
// JPA Repository 인터페이스

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 상담 기록 저장/조회
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    boolean existsByFileUploadNo(Long fileUploadNo);

    // 상담사 단건 연관만 함께 조회하여 서버 페이징을 유지
    @EntityGraph(attributePaths = "counselor")
    Page<ActivityLog> findByCustomer_CustomerId(
            Long customerId,
            Pageable pageable
    );

    Optional<ActivityLog> findByActivityIdAndCustomer_CustomerId(Long activityId, Long customerId);

    // 일반 상담 기록을 제외하고 가장 최근 신청 접수 원본 한 건만 조회
    Optional<ActivityLog> findFirstByCustomer_CustomerIdAndCreatedByAndActivityResultOrderByCreatedAtDescActivityIdDesc(
            Long customerId,
            String createdBy,
            String activityResult
    );

    // 담당 상담사가 없는 신청 접수 기록 조회
    List<ActivityLog> findByCustomer_CustomerIdAndCreatedByAndActivityResultAndCounselorIsNull(
            Long customerId,
            String createdBy,
            String activityResult
    );
}