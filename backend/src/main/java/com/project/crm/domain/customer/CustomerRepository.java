package com.project.crm.domain.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// 고객 리포지토리 (데이터베이스 접근)
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 상담사별 고객 조회
    List<Customer> findByCounselor_UserId(Long counselorId);
}
