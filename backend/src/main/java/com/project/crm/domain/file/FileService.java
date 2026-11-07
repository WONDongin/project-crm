package com.project.crm.domain.file;

import com.project.crm.domain.activity.*;
import com.project.crm.domain.customer.CustomerRepository;
import com.project.crm.domain.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

@Service @RequiredArgsConstructor @Transactional
public class FileService {
    private static final String CATEGORY = "activity_logs";
    private final FileRepository files;
    private final AttachmentStorage storage;
    private final ActivityLogRepository activities;
    private final CustomerRepository customers;
    private final UserRepository users;

    public record Attachment(Long fileId, String originalName, String contentType, long sizeBytes, LocalDateTime uploadedAt) {}
    public record Download(String name, String type, byte[] bytes) {}
    private record Access(User user, ActivityLog activity) {}

    private Access access(Long customerId, Long activityId, String email, boolean writing) {
        User user = users.findByEmailForWork(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ROLE_CONSULTANT".equals(user.getRoles()) || !"ACTIVE".equals(user.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // 고객 행 잠금으로 담당자 재배정 및 동시 첨부 개수 검사를 직렬화한다.
        customers.findAccessibleForConsultant(customerId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        ActivityLog log = activities.findByActivityIdAndCustomer_CustomerId(activityId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (writing && (!"CONSULTANT".equals(log.getCreatedBy()) || log.getCounselor() == null
                || !user.getUserId().equals(log.getCounselor().getUserId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 상담 기록의 첨부파일만 변경할 수 있습니다.");
        }
        return new Access(user, log);
    }
    public List<Attachment> list(Long customerId, Long activityId, String email) {
        var log = access(customerId, activityId, email, false).activity();
        if (log.getFileUploadNo() == null) return List.of();
        return files.findByFileUploadNoAndFileCategoryAndDelYnOrderByFileIdAsc(log.getFileUploadNo(), CATEGORY, "N").stream()
                .map(f -> new Attachment(f.getFileId(), f.getOriginalName(), contentType(f.getOriginalName()), f.getSizeBytes(), f.getUploadedAt())).toList();
    }
    public Long upload(Long customerId, Long activityId, String email, MultipartFile file) {
        var access = access(customerId, activityId, email, true);
        if (files.countByFileUploadNoAndFileCategoryAndDelYn(access.activity().getFileUploadNo(), CATEGORY, "N") >= 10) throw new ResponseStatusException(HttpStatus.CONFLICT, "기록당 최대 10개까지 첨부할 수 있습니다.");
        if (file.getSize() > AttachmentValidator.MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "파일당 10MB까지 첨부할 수 있습니다.");
        String name = AttachmentValidator.safeName(file.getOriginalFilename());
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(AttachmentValidator.MAX_BYTES + 1);
            AttachmentValidator.type(name, bytes);
            if (access.activity().getFileUploadNo() == null) {
                // 기존 그룹 번호는 유지하고, 새 그룹은 양수 난수로 생성한다.
                long group;
                do { group = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; }
                while (group == 0 || files.existsByFileUploadNo(group) || activities.existsByFileUploadNo(group));
                access.activity().setFileUploadNo(group);
            }
            var stored = storage.write(bytes);
            return files.save(FileMeta.builder().fileUploadNo(access.activity().getFileUploadNo()).fileCategory(CATEGORY)
                    .originalName(name).savedName(stored.name()).filePath(stored.directory())
                    .sizeBytes(bytes.length).delYn("N").uploadedAt(LocalDateTime.now()).build()).getFileId();
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일을 읽을 수 없습니다."); }
    }
    public Download download(Long customerId, Long activityId, Long fileId, String email) {
        var log = access(customerId, activityId, email, false).activity();
        var file = attachment(log, fileId);
        return new Download(file.getOriginalName(), contentType(file.getOriginalName()), storage.read(file));
    }
    public void delete(Long customerId, Long activityId, Long fileId, String email) {
        var log = access(customerId, activityId, email, true).activity();
        attachment(log, fileId).markDeleted();
        // 논리 삭제: 롤백과 복구를 위해 실제 파일은 보존한다. 별도 보존 정책에 따라 정리한다.
    }
    private FileMeta attachment(ActivityLog log, Long fileId) {
        if (log.getFileUploadNo() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return files.findByFileIdAndFileUploadNoAndFileCategoryAndDelYn(fileId, log.getFileUploadNo(), CATEGORY, "N")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private String contentType(String name) {
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return AttachmentValidator.TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
