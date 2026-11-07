package com.project.crm.domain.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class AttachmentStorage {
    private static final Logger log = LoggerFactory.getLogger(AttachmentStorage.class);
    private final Path root;

    public AttachmentStorage(@Value("${app.attachments.dir:./uploads/attachments}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public record Stored(String directory, String name) {}

    public Stored write(byte[] bytes) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("첨부파일 저장에는 트랜잭션이 필요합니다.");
        }
        String name = UUID.randomUUID().toString();
        Path target = root.resolve(name);
        boolean created = false;
        try {
            Files.createDirectories(root);
            // 경로는 외부 정적 리소스로 공개하지 않는다.
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                output.write(bytes);
            }
        } catch (IOException e) {
            if (created) cleanup(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "첨부파일을 저장할 수 없습니다.", e);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) cleanup(target);
                // 커밋 결과 불명인 경우 파일을 보존해 메타데이터의 유실을 방지한다.
            }
        });
        return new Stored(".", name);
    }

    public byte[] read(FileMeta file) {
        try {
            Path directory = Path.of(file.getFilePath());
            Path name = Path.of(file.getSavedName());
            if (name.isAbsolute() || name.getNameCount() != 1) throw new IOException("잘못된 저장 파일명");
            Path target = (directory.isAbsolute() ? directory : root.resolve(directory)).resolve(name).normalize();
            Path realRoot = root.toRealPath();
            if (!target.startsWith(root) || !target.toRealPath().startsWith(realRoot)
                    || !Files.isRegularFile(target)) throw new IOException("허용 경로 밖의 파일");
            try (var input = Files.newInputStream(target)) {
                byte[] bytes = input.readNBytes(AttachmentValidator.MAX_BYTES + 1);
                if (bytes.length > AttachmentValidator.MAX_BYTES) throw new IOException("허용 크기 초과");
                return bytes;
            }
        } catch (IOException | InvalidPathException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다.", e);
        }
    }

    private void cleanup(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { log.error("롤백 첨부파일 정리 실패: {}", path, e); }
    }
}
