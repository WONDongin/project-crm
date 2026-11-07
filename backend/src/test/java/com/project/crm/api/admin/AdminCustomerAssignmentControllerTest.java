package com.project.crm.api.admin;

import com.project.crm.config.security.*;
import com.project.crm.domain.customer.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCustomerController.class) @Import(SecurityConfig.class)
class AdminCustomerAssignmentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerService customers;
    @MockitoBean AdminCustomerAssignmentService assignments;
    @MockitoBean UserRepository users;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean com.project.crm.domain.customer.CustomerBrowseService browse;
    private UsernamePasswordAuthenticationToken login(String role) { return new UsernamePasswordAuthenticationToken("admin@example.com",null,List.of(new SimpleGrantedAuthority(role))); }
    @Test void explicitDeleteRequiresAdminAndUsesAuthenticatedActor() throws Exception {
        mvc.perform(delete("/admin/customers/10/counselor")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/admin/customers/10/counselor").with(authentication(login("ROLE_CONSULTANT")))).andExpect(status().isForbidden());
        mvc.perform(delete("/admin/customers/10/counselor").with(authentication(login("ROLE_ADMIN")))).andExpect(status().isOk());
        verify(assignments).unassignCounselor(10L,"admin@example.com");
    }
    @Test void emptyAssignmentRequestDoesNotImplicitlyUnassignCustomer() throws Exception {
        mvc.perform(patch("/admin/customers/10/counselor").with(authentication(login("ROLE_ADMIN"))).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(assignments);
    }
}
