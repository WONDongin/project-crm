package com.project.crm.api.consultant;

import com.project.crm.config.security.*;
import com.project.crm.domain.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultantAttachmentController.class) @Import(SecurityConfig.class)
class ConsultantAttachmentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean FileService files;
    @MockitoBean JwtUtil jwtUtil;
    private UsernamePasswordAuthenticationToken login(String role) {
        return new UsernamePasswordAuthenticationToken("owner@example.com","unused",List.of(new SimpleGrantedAuthority(role)));
    }
    @Test void multipartUsesAuthenticatedIdentityAndDownloadsAsAttachment() throws Exception {
        given(files.upload(eq(1L),eq(2L),eq("owner@example.com"),any())).willReturn(3L);
        mvc.perform(multipart("/consultant/customers/1/activities/2/attachments")
                .file(new MockMultipartFile("file","상담.pdf","application/pdf","%PDF-1.7".getBytes()))
                .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data").value(3));
        byte[] content="%PDF-1.7".getBytes();
        given(files.download(1L,2L,3L,"owner@example.com")).willReturn(new FileService.Download("상담.pdf","application/pdf",content));
        mvc.perform(get("/consultant/customers/1/activities/2/attachments/3").with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isOk()).andExpect(content().bytes(content))
                .andExpect(header().string("Content-Type","application/pdf"))
                .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(header().string("X-Content-Type-Options","nosniff"))
                .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(delete("/consultant/customers/1/activities/2/attachments/3").with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isOk());verify(files).delete(1L,2L,3L,"owner@example.com");
    }
    @Test void blocksAnonymousAdminAndMissingFile() throws Exception {
        mvc.perform(get("/consultant/customers/1/activities/2/attachments")).andExpect(status().isForbidden());
        mvc.perform(get("/consultant/customers/1/activities/2/attachments").with(authentication(login("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/consultant/customers/1/activities/2/attachments").with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isBadRequest());
    }
}
