package com.project.crm.domain.user;
// JPA Repository 인터페이스

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndStatus(String email, String status);
}
