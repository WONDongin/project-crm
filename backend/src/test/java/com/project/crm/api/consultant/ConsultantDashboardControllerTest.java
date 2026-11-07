package com.project.crm.api.consultant;

import com.project.crm.config.security.*;
import com.project.crm.domain.customer.ConsultantDashboardService;
import com.project.crm.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultantDashboardController.class) @Import(SecurityConfig.class)
class ConsultantDashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ConsultantDashboardService dashboard;
    @MockitoBean UserRepository users;
    @MockitoBean JwtUtil jwtUtil;
    @Test void authenticatesRoleAndDerivesIdentityFromLogin() throws Exception {
        mvc.perform(get("/consultant/dashboard")).andExpect(status().isUnauthorized());
        var admin=new UsernamePasswordAuthenticationToken("admin@example.com",null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(get("/consultant/dashboard").with(authentication(admin))).andExpect(status().isForbidden());
        var owner=new UsernamePasswordAuthenticationToken("owner@example.com",null,List.of(new SimpleGrantedAuthority("ROLE_CONSULTANT")));
        when(dashboard.get("owner@example.com")).thenReturn(new ConsultantDashboardService.Dashboard(0,List.of(),List.of()));
        mvc.perform(get("/consultant/dashboard").with(authentication(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCustomers").value(0));
        verify(dashboard).get("owner@example.com");
    }
}
