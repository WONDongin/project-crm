package com.project.crm.domain.activity;
// JPA Repository 인터페이스

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
// 상담 기록 저장/조회
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 상담사 단건 연관만 함께 조회하여 서버 페이징을 유지한다.
    @EntityGraph(attributePaths = "counselor")
    Page<ActivityLog> findByCustomer_CustomerId(Long customerId, Pageable pageable);

    // 일반 상담 기록을 제외하고 가장 최근 신청 접수 원본 한 건만 조회한다.
    Optional<ActivityLog> findFirstByCustomer_CustomerIdAndCreatedByAndActivityResultOrderByCreatedAtDescActivityIdDesc(
            Long customerId, String createdBy, String activityResult
    );
}


