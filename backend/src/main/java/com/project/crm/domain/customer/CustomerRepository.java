package com.project.crm.domain.customer;

import org.springframework.data.jpa.repository.JpaRepository;

// 고객 리포지토리 (데이터베이스 접근)
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
