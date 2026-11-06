package com.project.crm.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileMeta, Long> {
    interface Info {
        Long getFileId(); String getOriginalName(); String getContentType();
        long getSizeBytes(); LocalDateTime getUploadedAt();
    }
    // 닫힌 projection으로 파일 목록 조회 시 바이너리 본문을 제외한다.
    List<Info> findByActivity_ActivityIdOrderByFileIdAsc(Long activityId);
    long countByActivity_ActivityId(Long activityId);
    Optional<FileMeta> findByFileIdAndActivity_ActivityId(Long fileId, Long activityId);
}
