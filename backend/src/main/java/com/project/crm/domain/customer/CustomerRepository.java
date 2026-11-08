package com.project.crm.domain.customer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 고객 리포지토리 (데이터베이스 접근)
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    @Override
    @EntityGraph(attributePaths = "counselor")
    Page<Customer> findAll(
            Specification<Customer> specification,
            Pageable pageable);

    long countByCounselorIsNull();

    @EntityGraph(attributePaths = "counselor")
    Page<Customer> findByCounselorIsNull(Pageable pageable);

    @Query("select c.status as status, count(c) as customerCount from Customer c group by c.status")
    List<StatusCount> countAllStatuses();

    interface StatusCount { String getStatus(); long getCustomerCount(); }
    @Query("select c.status as status, count(c) as customerCount from Customer c where c.counselor.userId = :id group by c.status")
    List<StatusCount> countStatusesForCounselor(@Param("id") Long id);

    @EntityGraph(attributePaths = "counselor")
    Page<Customer> findByCounselor_UserIdAndLastActivityAtIsNotNull(
            Long id, Pageable pageable);

    interface AssignmentCount { Long getUserId(); long getCustomerCount(); }
    @Query("select c.counselor.userId as userId, count(c) as customerCount from Customer c where c.counselor is not null group by c.counselor.userId")
    List<AssignmentCount> countAssignments();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.counselor.userId = :userId order by c.customerId")
    List<Customer> findAssignedForUpdate(@Param("userId") Long userId);

    // 상담사별 고객 조회
    List<Customer> findByCounselor_UserId(Long counselorId);

    // 상세 조회에서는 변경용 비관적 쓰기 잠금을 사용하지 않는다.
    Optional<Customer> findByCustomerIdAndCounselor_UserId(Long customerId, Long counselorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customer from Customer customer where customer.customerId = :customerId")
    Optional<Customer> findByIdForUpdate(@Param("customerId") Long customerId);

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

    @Query("""
        select c from Customer c
        where replace(replace(replace(replace(replace(replace(c.phone, '-', ''), ' ', ''), '(', ''), ')', ''), '.', ''), '+', '') = :phone
          and (c.birthDate = :birthDate or (c.birthDate is null and :birthDate is null))
          and c.customerId <> :excludedId
        """)
    List<Customer> findIdentityMatches(@Param("phone") String phone,
            @Param("birthDate") LocalDate birthDate, @Param("excludedId") Long excludedId);

    // 상담신청_기존 고객 여부 판단
    Optional<Customer> findByPhoneAndBirthDate(
        String phone,
        LocalDate birthDate
    );
}

