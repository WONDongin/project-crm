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

@Service @RequiredArgsConstructor @Transactional
public class FileService {
    private final FileRepository files;
    private final ActivityLogRepository activities;
    private final CustomerRepository customers;
    private final UserRepository users;

    public record Attachment(Long fileId, String originalName, String contentType, long sizeBytes, LocalDateTime uploadedAt) {}
    public record Download(String name, String type, byte[] bytes) {}
    private record Access(User user, ActivityLog activity) {}

    private Access access(Long customerId, Long activityId, String email, boolean writing) {
        User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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
        access(customerId, activityId, email, false);
        return files.findByActivity_ActivityIdOrderByFileIdAsc(activityId).stream()
                .map(f -> new Attachment(f.getFileId(), f.getOriginalName(), f.getContentType(), f.getSizeBytes(), f.getUploadedAt())).toList();
    }
    public Long upload(Long customerId, Long activityId, String email, MultipartFile file) {
        var access = access(customerId, activityId, email, true);
        if (files.countByActivity_ActivityId(activityId) >= 10) throw new ResponseStatusException(HttpStatus.CONFLICT, "기록당 최대 10개까지 첨부할 수 있습니다.");
        if (file.getSize() > AttachmentValidator.MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "파일당 10MB까지 첨부할 수 있습니다.");
        String name = AttachmentValidator.safeName(file.getOriginalFilename());
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(AttachmentValidator.MAX_BYTES + 1);
            String type = AttachmentValidator.type(name, bytes);
            return files.save(FileMeta.builder().activity(access.activity()).originalName(name).contentType(type)
                    .sizeBytes(bytes.length).uploadedBy(access.user().getUserId()).uploadedAt(LocalDateTime.now()).content(bytes).build()).getFileId();
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일을 읽을 수 없습니다."); }
    }
    public Download download(Long customerId, Long activityId, Long fileId, String email) {
        access(customerId, activityId, email, false);
        var file = files.findByFileIdAndActivity_ActivityId(fileId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Download(file.getOriginalName(), file.getContentType(), file.getContent());
    }
    public void delete(Long customerId, Long activityId, Long fileId, String email) {
        access(customerId, activityId, email, true);
        var file = files.findByFileIdAndActivity_ActivityId(fileId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        files.delete(file);
    }
}
