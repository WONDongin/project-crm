package com.project.crm.domain.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.ZipInputStream;

final class AttachmentValidator {
    static final int MAX_BYTES = 10 * 1024 * 1024;
    static final Map<String, String> TYPES = Map.ofEntries(
        Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"), Map.entry("webp", "image/webp"), Map.entry("pdf", "application/pdf"),
        Map.entry("txt", "text/plain"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));

    static String type(String filename, byte[] bytes) {
        if (bytes.length == 0) throw bad("빈 파일은 첨부할 수 없습니다.");
        if (bytes.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "파일당 10MB까지 첨부할 수 있습니다.");
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String type = TYPES.get(ext);
        boolean valid = switch (ext) {
            case "png" -> starts(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "jpg", "jpeg" -> starts(bytes, new byte[]{(byte)255,(byte)216,(byte)255});
            case "gif" -> prefix(bytes, "GIF87a") || prefix(bytes, "GIF89a");
            case "webp" -> prefix(bytes, "RIFF") && bytes.length >= 12 && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "pdf" -> prefix(bytes, "%PDF-");
            case "txt" -> validText(bytes);
            case "docx", "xlsx", "pptx" -> validOffice(bytes, ext);
            default -> false;
        };
        if (type == null || !valid) throw bad("지원하지 않는 형식이거나 파일 내용과 확장자가 다릅니다.");
        return type;
    }
    static String safeName(String value) {
        if (value == null || value.isBlank()) throw bad("파일명이 없습니다.");
        String name = value.replace('\\', '/'); name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}]", "_");
        if (name.isBlank() || name.length() > 200) throw bad("파일명은 200자 이하로 입력해 주세요.");
        return name;
    }
    private static boolean prefix(byte[] bytes, String prefix) { return starts(bytes, prefix.getBytes(StandardCharsets.US_ASCII)); }
    private static boolean starts(byte[] b, byte[] prefix) {
        if (b.length < prefix.length) return false;
        for (int i=0;i<prefix.length;i++) if (b[i]!=prefix[i]) return false;
        return true;
    }
    private static boolean validText(byte[] b) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();
            return text.chars().noneMatch(c -> c < 32 && c != '\n' && c != '\r' && c != '\t');
        } catch (CharacterCodingException e) { return false; }
    }
    private static boolean validOffice(byte[] b, String ext) {
        if (!prefix(b, "PK")) return false;
        String main = switch(ext) { case "docx" -> "word/document.xml"; case "xlsx" -> "xl/workbook.xml"; default -> "ppt/presentation.xml"; };
        boolean contentTypes=false, mainFound=false; long expanded=0; int entries=0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(b))) {
            java.util.zip.ZipEntry entry; byte[] buffer = new byte[8192];
            while ((entry=zip.getNextEntry()) != null) {
                if (++entries > 2000) return false;
                contentTypes |= entry.getName().equals("[Content_Types].xml");
                mainFound |= entry.getName().equals(main);
                int n; while ((n=zip.read(buffer))!=-1) { expanded+=n; if (expanded > 100L*1024*1024) return false; }
            }
            return contentTypes && mainFound;
        } catch (IOException e) { return false; }
    }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
