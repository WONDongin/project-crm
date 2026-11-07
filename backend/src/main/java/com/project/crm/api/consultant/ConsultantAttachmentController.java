package com.project.crm.api.consultant;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/consultant/customers/{customerId}/activities/{activityId}/attachments")
public class ConsultantAttachmentController {
    private final FileService files;
    @GetMapping public ApiResponse<List<FileService.Attachment>> list(@PathVariable Long customerId,
            @PathVariable Long activityId, Authentication auth) {
        return ApiResponse.success(files.list(customerId, activityId, auth.getName()));
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> upload(@PathVariable Long customerId, @PathVariable Long activityId,
            @RequestPart("file") MultipartFile file, Authentication auth) {
        return ApiResponse.success(files.upload(customerId, activityId, auth.getName(), file));
    }
    @GetMapping("/{fileId}") public ResponseEntity<byte[]> download(@PathVariable Long customerId,
            @PathVariable Long activityId, @PathVariable Long fileId, Authentication auth) {
        var file = files.download(customerId, activityId, fileId, auth.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore())
                .body(file.bytes());
    }
    @DeleteMapping("/{fileId}") public ApiResponse<Void> delete(@PathVariable Long customerId,
            @PathVariable Long activityId, @PathVariable Long fileId, Authentication auth) {
        files.delete(customerId, activityId, fileId, auth.getName());
        return ApiResponse.success(null, "첨부파일이 삭제되었습니다.");
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> tooLarge() { return ApiResponse.fail("파일당 10MB까지 첨부할 수 있습니다."); }
}
