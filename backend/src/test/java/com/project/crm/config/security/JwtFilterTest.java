package com.project.crm.config.security;

import com.project.crm.domain.user.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {
    @Mock JwtUtil jwt;
    @Mock UserRepository users;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private User account() { return User.builder().userId(7L).email("owner@example.com").roles("ROLE_CONSULTANT").status("ACTIVE").authVersion(3).build(); }
    private void token(Long id,Long version) {
        when(jwt.validateToken("token")).thenReturn(true);when(jwt.getEmail("token")).thenReturn("owner@example.com");
        when(jwt.getUserId("token")).thenReturn(id);when(jwt.getAuthVersion("token")).thenReturn(version);
        lenient().when(jwt.getRole("token")).thenReturn("ROLE_CONSULTANT");
    }
    private void filter() throws Exception {
        var request=new MockHttpServletRequest();request.setCookies(new Cookie("accessToken","token"));
        new JwtFilter(jwt,users).doFilter(request,new MockHttpServletResponse(),(req,res)->{});
    }
    @Test void authenticatesOnlyCurrentActiveIdentityAndVersion() throws Exception {
        token(7L,3L);when(users.findById(7L)).thenReturn(Optional.of(account()));filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("owner@example.com");
    }
    @Test void inactiveAccountCannotUseExistingToken() throws Exception {
        token(7L,3L);var user=account();user.setStatus("INACTIVE");when(users.findById(7L)).thenReturn(Optional.of(user));filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void oldVersionStaysRejectedAfterReactivationOrPasswordReset() throws Exception {
        token(7L,2L);when(users.findById(7L)).thenReturn(Optional.of(account()));filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void tokenRoleCannotOverrideCurrentDatabaseRole() throws Exception {
        token(7L,3L);var user=account();user.setRoles("ROLE_ADMIN");when(users.findById(7L)).thenReturn(Optional.of(user));filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void legacyTokensWithoutIdentityAndVersionRequireLogin() throws Exception {
        token(null,null);filter();verifyNoInteractions(users);assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void deletedUserAndChangedEmailAreRejected() throws Exception {
        token(7L,3L);when(users.findById(7L)).thenReturn(Optional.empty());filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        var changed=account();changed.setEmail("changed@example.com");when(users.findById(7L)).thenReturn(Optional.of(changed));filter();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
