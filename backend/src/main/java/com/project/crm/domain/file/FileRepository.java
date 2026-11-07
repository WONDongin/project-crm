package com.project.crm.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileMeta, Long> {
    List<FileMeta> findByFileUploadNoAndFileCategoryAndDelYnOrderByFileIdAsc(Long group, String category, String delYn);
    long countByFileUploadNoAndFileCategoryAndDelYn(Long group, String category, String delYn);
    Optional<FileMeta> findByFileIdAndFileUploadNoAndFileCategoryAndDelYn(Long id, Long group, String category, String delYn);
    boolean existsByFileUploadNo(Long group);
}
