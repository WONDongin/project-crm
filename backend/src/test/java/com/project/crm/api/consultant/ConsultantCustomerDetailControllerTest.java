package com.project.crm.api.consultant;

import com.project.crm.config.security.JwtUtil;
import com.project.crm.config.security.SecurityConfig;
import com.project.crm.domain.customer.ConsultantCustomerDetailService;
import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.ConsultantCustomerDetailResponseDto;
import com.project.crm.domain.user.User;
import com.project.crm.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultantCustomerController.class)
@Import(SecurityConfig.class)
class ConsultantCustomerDetailControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerService customerService;
    @MockitoBean ConsultantCustomerDetailService detailService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean JwtUtil jwtUtil;

    @Test
    void derivesCounselorFromLoginAndIgnoresForgedCounselorParameter() throws Exception {
        given(userRepository.findByEmail("owner@example.com"))
                .willReturn(Optional.of(User.builder().userId(7L).build()));
        given(detailService.getDetail(10L, 7L)).willReturn(new ConsultantCustomerDetailResponseDto(
                10L, "김고객", "010-1234-5678", null, null, "JAVA", "WEB", "NEW",
                null, "담당 상담사", null, null,
                new ConsultantCustomerDetailResponseDto.Application(1L, "PM", "과정 문의", null)
        ));

        mockMvc.perform(get("/consultant/customers/10?counselorId=999")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").value(10))
                .andExpect(jsonPath("$.data.application.content").value("과정 문의"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        verify(detailService).getDetail(10L, 7L);
    }

    @Test
    void blocksAnonymousAndAdminBeforeReadingCustomerData() throws Exception {
        mockMvc.perform(get("/consultant/customers/10")).andExpect(status().isForbidden());
        mockMvc.perform(get("/consultant/customers/10")
                        .with(authentication(login("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(detailService, userRepository);
    }

    @Test
    void propagatesOwnershipDenial() throws Exception {
        given(userRepository.findByEmail("owner@example.com"))
                .willReturn(Optional.of(User.builder().userId(7L).build()));
        given(detailService.getDetail(10L, 7L))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mockMvc.perform(get("/consultant/customers/10")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsLoginWhoseUserNoLongerExists() throws Exception {
        given(userRepository.findByEmail("owner@example.com")).willReturn(Optional.empty());
        mockMvc.perform(get("/consultant/customers/10")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(detailService);
    }

    private UsernamePasswordAuthenticationToken login(String role) {
        return new UsernamePasswordAuthenticationToken("owner@example.com", null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
