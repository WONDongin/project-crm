package com.project.crm.domain.user;
// JPA Repository 인터페이스

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByEmailAndStatus(String email, String status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForWork(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from User u where u.userId = :id")
    Optional<User> findByIdForWork(@Param("id") Long id);

    // 계정 변경을 직렬화하여 마지막 활성 관리자와 상태 전환을 보호한다.
    // 담당자 배정도 같은 순서(users -> customers)를 사용한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u order by u.userId")
    List<User> findAllForAdministration();

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);

    // 배정 가능한 상담사 목록 조회
    List<User> findByRolesAndStatusOrderByNameAscUserIdAsc(
            String roles,
            String status
    );
}