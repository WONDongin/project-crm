package com.project.crm.api.admin;

import com.project.crm.config.security.*;
import com.project.crm.domain.user.*;
import com.project.crm.domain.user.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class) @Import(SecurityConfig.class)
class AdminUserControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminUserService service;
    @MockitoBean UserRepository users;
    @MockitoBean JwtUtil jwtUtil;
    private UsernamePasswordAuthenticationToken login(String role) { return new UsernamePasswordAuthenticationToken("admin@example.com",null,List.of(new SimpleGrantedAuthority(role))); }
    private final String create="""
      {"name":"상담사","email":"new@example.com","password":"newpass123!","roles":"ROLE_CONSULTANT","specialty":"BACKEND","status":"ACTIVE"}
      """;
    @Test void adminCreatesAccountWithoutReturningPassword() throws Exception {
        when(service.create(eq("admin@example.com"),any())).thenReturn(UserResponseDto.from(User.builder().userId(5L).name("상담사").email("new@example.com").roles("ROLE_CONSULTANT").specialty("BACKEND").status("ACTIVE").build()));
        mvc.perform(post("/admin/users").with(authentication(login("ROLE_ADMIN"))).contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.email").value("new@example.com")).andExpect(jsonPath("$.data.password").doesNotExist()).andExpect(jsonPath("$.data.authVersion").doesNotExist());
    }
    @Test void rejectsAnonymousConsultantAndInvalidPassword() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/users").with(authentication(login("ROLE_CONSULTANT")))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users").with(authentication(login("ROLE_ADMIN"))).contentType(MediaType.APPLICATION_JSON).content(create.replace("newpass123!","short")))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void returnsExplicitConflictAndUsesAuthenticatedActorForReset() throws Exception {
        when(service.create(anyString(),any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"이미 사용 중인 이메일입니다."));
        mvc.perform(post("/admin/users").with(authentication(login("ROLE_ADMIN"))).contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        mvc.perform(post("/admin/users/5/password").with(authentication(login("ROLE_ADMIN"))).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"resetpass123!\"}"))
                .andExpect(status().isOk());verify(service).resetPassword(5L,"admin@example.com","resetpass123!");
    }

    @Test void bindsSearchParametersAndReturnsPagedSafeAccountData() throws Exception {
        var row = UserResponseDto.from(User.builder().userId(5L).name("김상담").email("kim@example.com")
                .roles("ROLE_CONSULTANT").specialty("BACKEND").status("ACTIVE").build(), 3);
        when(service.list("admin@example.com", "김", "ROLE_CONSULTANT", "ACTIVE", 1, 10))
                .thenReturn(new UserPageResponseDto(List.of(row), 1, 10, 11, 2));
        mvc.perform(get("/admin/users").with(authentication(login("ROLE_ADMIN")))
                .param("keyword", "김").param("roles", "ROLE_CONSULTANT").param("status", "ACTIVE").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(11))
                .andExpect(jsonPath("$.data.number").value(1)).andExpect(jsonPath("$.data.content[0].assignedCustomerCount").value(3))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].authVersion").doesNotExist());
        verify(service).list("admin@example.com", "김", "ROLE_CONSULTANT", "ACTIVE", 1, 10);
        mvc.perform(get("/admin/users").with(authentication(login("ROLE_ADMIN"))).param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test void cookieAuthenticationRejectsRevokedVersionOnProtectedEndpoint() throws Exception {
        when(jwtUtil.validateToken("signed")).thenReturn(true);
        when(jwtUtil.getEmail("signed")).thenReturn("admin@example.com");
        when(jwtUtil.getUserId("signed")).thenReturn(1L);
        when(jwtUtil.getAuthVersion("signed")).thenReturn(2L);
        when(jwtUtil.getRole("signed")).thenReturn("ROLE_ADMIN");
        var user=User.builder().userId(1L).email("admin@example.com").roles("ROLE_ADMIN").status("ACTIVE").authVersion(2L).build();
        when(users.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(service.list("admin@example.com", "", "", "", 0, 10)).thenReturn(new UserPageResponseDto(List.of(),0,10,0,0));
        mvc.perform(get("/admin/users").cookie(new jakarta.servlet.http.Cookie("accessToken","signed"))).andExpect(status().isOk());
        user.setAuthVersion(3L);
        mvc.perform(get("/admin/users").cookie(new jakarta.servlet.http.Cookie("accessToken","signed"))).andExpect(status().isUnauthorized());
    }
}
