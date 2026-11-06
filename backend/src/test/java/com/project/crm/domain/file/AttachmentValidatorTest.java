package com.project.crm.domain.file;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class AttachmentValidatorTest {
    @Test void checksExtensionSignatureAndSize() {
        assertThat(AttachmentValidator.type("사진.PNG", new byte[]{(byte)137,80,78,71,13,10,26,10})).isEqualTo("image/png");
        assertThat(AttachmentValidator.type("문서.pdf", "%PDF-1.7\n".getBytes())).isEqualTo("application/pdf");
        assertThat(AttachmentValidator.type("메모.txt", "상담 메모".getBytes(java.nio.charset.StandardCharsets.UTF_8))).isEqualTo("text/plain");
        for (String name : java.util.List.of("a.exe", "a.html", "a.png", "a.docx")) {
            assertThatThrownBy(() -> AttachmentValidator.type(name, "<html>fake</html>".getBytes())).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> AttachmentValidator.type("empty.txt", new byte[0])).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> AttachmentValidator.type("large.txt", new byte[10485761]))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
        assertThatThrownBy(() -> AttachmentValidator.type("binary.txt", new byte[]{0,1,2})).isInstanceOf(ResponseStatusException.class);
    }
    @Test void checksOfficeStructureAndSanitizesNames() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (String path : java.util.List.of("[Content_Types].xml", "word/document.xml")) {
                zip.putNextEntry(new ZipEntry(path)); zip.write("<xml/>".getBytes()); zip.closeEntry();
            }
        }
        assertThat(AttachmentValidator.type("상담.docx", bytes.toByteArray())).contains("wordprocessingml");
        assertThatThrownBy(() -> AttachmentValidator.type("상담.xlsx", bytes.toByteArray())).isInstanceOf(ResponseStatusException.class);
        assertThat(AttachmentValidator.safeName("C:\\fakepath\\상담.pdf")).isEqualTo("상담.pdf");
        assertThat(AttachmentValidator.safeName("../상담\r\n.pdf")).isEqualTo("상담__.pdf");
        assertThatThrownBy(() -> AttachmentValidator.safeName("a".repeat(201))).isInstanceOf(ResponseStatusException.class);
    }
}
