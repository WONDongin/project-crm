package com.project.crm.domain.activity;
// JPA Repository 인터페이스

import org.springframework.data.jpa.repository.JpaRepository;
// 상담 기록 저장/조회
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
}
