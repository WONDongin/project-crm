package com.project.crm.domain.customer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 고객 리포지토리 (데이터베이스 접근)
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 상담사별 고객 조회
    List<Customer> findByCounselor_UserId(Long counselorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select customer
        from Customer customer
        where customer.customerId = :customerId
          and customer.counselor.userId = :counselorId
        """)
    Optional<Customer> findAccessibleForConsultant(
        @Param("customerId") Long customerId,
        @Param("counselorId") Long counselorId
    );

    // 상담신청_기존 고객 여부 판단
    Optional<Customer> findByPhoneAndBirthDate(
        String phone,
        LocalDate birthDate
    );
}
