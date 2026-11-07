package com.project.crm.domain.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

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

    @Test void readsManagedFileAndRejectsTraversalAndSymlinkEscape() throws Exception {
        var root = Files.createDirectory(directory.resolve("managed"));
        var storage = new AttachmentStorage(root.toString());
        Files.write(root.resolve("valid"), new byte[]{1,2,3});
        assertThat(storage.read(FileMeta.builder().filePath(".").savedName("valid").build()))
                .containsExactly(1,2,3);
        var outside = Files.write(directory.resolve("private"), new byte[]{4});
        assertNotFound(storage, FileMeta.builder().filePath("..").savedName("private").build());
        assertNotFound(storage, FileMeta.builder().filePath(".").savedName("../private").build());
        Files.createSymbolicLink(root.resolve("link"), outside);
        assertNotFound(storage, FileMeta.builder().filePath(".").savedName("link").build());
    }
    private void assertNotFound(AttachmentStorage storage, FileMeta file) {
        assertThatThrownBy(() -> storage.read(file)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
}
