package com.project.crm.domain.user;
// JPA Repository 인터페이스

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, String status);

    // 배정 가능한 상담사 목록 조회
    List<User> findByRolesAndStatusOrderByNameAscUserIdAsc(
            String roles,
            String status
    );
}