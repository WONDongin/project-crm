package com.project.crm.domain.customer;
// 고객 엔티티

import com.project.crm.domain.user.User;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 100)
    private String email;

    @Column(name = "interest_course", nullable = false, length = 30)
    private String interestCourse;

    @Column(name = "lead_source", nullable = false, length = 30)
    private String leadSource;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "customer_tag", length = 50)
    private String customerTag;

    // 담당 상담사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_counselor_id")
    private User counselor;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}