package com.project.crm.api.consult;

import com.project.crm.config.security.*;
import com.project.crm.domain.customer.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultController.class)
@Import(SecurityConfig.class)
class ConsultControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerService service;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean com.project.crm.domain.user.UserRepository userRepository;
    private final String body = """
        {"name":"신청자","birthDate":"1997-12-28","phone":"010-1111-1111",
         "interestCourse":"JAVA","consultType":"PHONE","preferredTime":"PM",
         "leadSource":"WEB","content":"상담 신청"}
        """;
    @Test void concurrentUniqueConflictReturns409InsteadOf500() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate customer")).when(service).applyConsult(any());
        mvc.perform(post("/consult/apply").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
    @Test void oversizedNameIsRejectedBeforeCallingService() throws Exception {
        mvc.perform(post("/consult/apply").contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("신청자","가".repeat(51))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
