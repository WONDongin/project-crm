package com.project.crm.domain.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AttachmentStorageTest {
    @TempDir Path directory;

    @Test void rollbackRemovesNewFileWhileCommitPreservesIt() throws Exception {
        var storage = new AttachmentStorage(directory.toString());
        for (int status : new int[]{TransactionSynchronization.STATUS_ROLLED_BACK, TransactionSynchronization.STATUS_COMMITTED}) {
            TransactionSynchronizationManager.initSynchronization();
            try {
                var saved = storage.write("%PDF-test".getBytes());
                Path path = directory.resolve(saved.name());
                assertThat(Files.exists(path)).isTrue();
                for (var callback : TransactionSynchronizationManager.getSynchronizations()) callback.afterCompletion(status);
                assertThat(Files.exists(path)).isEqualTo(status == TransactionSynchronization.STATUS_COMMITTED);
            } finally { TransactionSynchronizationManager.clearSynchronization(); }
        }
    }

    @Test void requiresTransactionBeforeWriting() throws Exception {
        assertThatThrownBy(() -> new AttachmentStorage(directory.toString()).write(new byte[]{1}))
                .isInstanceOf(IllegalStateException.class);
        try (var paths = Files.list(directory)) { assertThat(paths.count()).isZero(); }
    }

    @Test void readsManagedFileAndRejectsTraversal() throws Exception {
        var root = Files.createDirectory(directory.resolve("managed"));
        var storage = new AttachmentStorage(root.toString());
        Files.write(root.resolve("valid"), new byte[]{1,2,3});
        assertThat(storage.read(FileMeta.builder().filePath(".").savedName("valid").build()))
                .containsExactly(1,2,3);
        Files.write(directory.resolve("private"), new byte[]{4});
        assertNotFound(storage, FileMeta.builder().filePath("..").savedName("private").build());
        assertNotFound(storage, FileMeta.builder().filePath(".").savedName("../private").build());
    }

    @Test void rejectsSymlinkEscapeWhenEnvironmentAllowsLinks() throws Exception {
        var root = Files.createDirectory(directory.resolve("managed"));
        var storage = new AttachmentStorage(root.toString());
        var outside = Files.write(directory.resolve("private"), new byte[]{4});
        try {
            Files.createSymbolicLink(root.resolve("link"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            // 링크 생성 능력만 확인한다. 아래 저장소 검증의 실패는 건너뛰지 않는다.
            assumeTrue(false, "이 환경은 심볼릭 링크 생성 권한 또는 지원이 없습니다: " + exception.getMessage());
        }
        assertNotFound(storage, FileMeta.builder().filePath(".").savedName("link").build());
    }
    private void assertNotFound(AttachmentStorage storage, FileMeta file) {
        assertThatThrownBy(() -> storage.read(file)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
}
