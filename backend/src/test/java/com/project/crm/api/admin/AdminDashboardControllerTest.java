package com.project.crm.api.admin;

import com.project.crm.config.security.*;
import com.project.crm.domain.customer.AdminDashboardService;
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

@WebMvcTest(AdminDashboardController.class) @Import(SecurityConfig.class)
class AdminDashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminDashboardService dashboard;
    @MockitoBean UserRepository users;
    @MockitoBean JwtUtil jwtUtil;
    @Test void authenticatesRoleAndDerivesIdentityFromLogin() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isUnauthorized());
        var consultant=new UsernamePasswordAuthenticationToken("consultant@example.com",null,List.of(new SimpleGrantedAuthority("ROLE_CONSULTANT")));
        mvc.perform(get("/admin/dashboard").with(authentication(consultant))).andExpect(status().isForbidden());
        var admin=new UsernamePasswordAuthenticationToken("admin@example.com",null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(dashboard.get("admin@example.com")).thenReturn(new AdminDashboardService.Dashboard(0,0,0,0,java.time.LocalDate.now(),List.of(),List.of()));
        mvc.perform(get("/admin/dashboard").with(authentication(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCustomers").value(0));
        verify(dashboard).get("admin@example.com");
    }
}
