package com.project.crm.domain.activity;

import com.project.crm.domain.customer.Customer;
import com.project.crm.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 상담 기록 엔티티
@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    // 고객
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // 상담사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id")
    private User counselor;

    // 생성 주체
    @Column(name = "created_by", nullable = false, length = 20)
    private String createdBy;

    // 상담 유형
    @Column(name = "consult_type", length = 20)
    private String consultType;

    // 상담 결과
    @Column(name = "activity_result", nullable = false, length = 20)
    private String activityResult;

    // 상담 내용
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // 다음 액션
    @Column(name = "next_action", length = 255)
    private String nextAction;

    // 첨부파일 번호
    @Column(name = "file_upload_no")
    private Long fileUploadNo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}