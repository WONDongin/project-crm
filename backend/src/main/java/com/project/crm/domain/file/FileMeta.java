package com.project.crm.domain.file;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_mng")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_seq") private Long fileId;
    @Column(name = "file_upload_no", nullable = false) private Long fileUploadNo;
    @Column(name = "file_category", nullable = false, length = 100) private String fileCategory;
    @Column(name = "file_nm", nullable = false, length = 255) private String originalName;
    @Column(name = "file_save_nm", nullable = false, length = 255) private String savedName;
    @Column(name = "file_path", nullable = false, length = 255) private String filePath;
    @Column(name = "file_size", nullable = false) private long sizeBytes;
    @Column(name = "del_yn", nullable = false, length = 1) private String delYn;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime uploadedAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public void markDeleted() {
        delYn = "Y";
        updatedAt = LocalDateTime.now();
    }
}
