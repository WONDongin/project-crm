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
import com.project.crm.domain.activity.dto.ActivityLogPageResponseDto;
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

    @Test
    void historyUsesAuthenticatedCounselorAndReturnsPageMetadata() throws Exception {
        given(userRepository.findByEmail("owner@example.com"))
                .willReturn(Optional.of(User.builder().userId(7L).build()));
        given(detailService.getHistory(10L, 7L, 1)).willReturn(new ActivityLogPageResponseDto(
                List.of(new ActivityLogPageResponseDto.Entry(3L, "SYSTEM", null, null,
                        "RECEIVED", "PM", "신청 내용", null, null)), 1, 5, 6, 2));
        mockMvc.perform(get("/consultant/customers/10/activities?page=1&size=100&counselorId=999&sort=activityId,asc")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].content").value("신청 내용"))
                .andExpect(jsonPath("$.data.content[0].counselor").doesNotExist());
        verify(detailService).getHistory(10L, 7L, 1);
    }

    @Test
    void historyDefaultsToFirstPageAndRejectsMalformedPage() throws Exception {
        given(userRepository.findByEmail("owner@example.com"))
                .willReturn(Optional.of(User.builder().userId(7L).build()));
        given(detailService.getHistory(10L, 7L, 0))
                .willReturn(new ActivityLogPageResponseDto(List.of(), 0, 5, 0, 0));
        mockMvc.perform(get("/consultant/customers/10/activities")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty());
        mockMvc.perform(get("/consultant/customers/10/activities?page=abc")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isBadRequest());
        given(detailService.getHistory(10L, 7L, -1))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        mockMvc.perform(get("/consultant/customers/10/activities?page=-1")
                        .with(authentication(login("ROLE_CONSULTANT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyBlocksAnonymousAdminMissingUserAndWrongOwner() throws Exception {
        mockMvc.perform(get("/consultant/customers/10/activities")).andExpect(status().isForbidden());
        mockMvc.perform(get("/consultant/customers/10/activities")
                        .with(authentication(login("ROLE_ADMIN")))).andExpect(status().isForbidden());
        verifyNoInteractions(detailService, userRepository);
        given(userRepository.findByEmail("owner@example.com")).willReturn(Optional.empty());
        mockMvc.perform(get("/consultant/customers/10/activities")
                        .with(authentication(login("ROLE_CONSULTANT")))).andExpect(status().isUnauthorized());
        given(userRepository.findByEmail("owner@example.com"))
                .willReturn(Optional.of(User.builder().userId(7L).build()));
        given(detailService.getHistory(10L, 7L, 0))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mockMvc.perform(get("/consultant/customers/10/activities")
                        .with(authentication(login("ROLE_CONSULTANT")))).andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken login(String role) {
        return new UsernamePasswordAuthenticationToken("owner@example.com", null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}

