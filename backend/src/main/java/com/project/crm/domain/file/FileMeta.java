package com.project.crm.domain.file;

import com.project.crm.domain.activity.ActivityLog;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_attachments")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "activity_id", nullable = false)
    private ActivityLog activity;
    @Column(nullable = false, length = 200) private String originalName;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false) private Long uploadedBy;
    @Column(nullable = false) private LocalDateTime uploadedAt;
    @Lob @Column(nullable = false) private byte[] content;
}
