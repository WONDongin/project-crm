package com.project.crm.api.admin;

import com.project.crm.config.security.*;
import com.project.crm.domain.customer.*;
import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
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

@WebMvcTest(AdminCustomerController.class) @Import(SecurityConfig.class)
class CustomerBrowseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerBrowseService browse;
    @MockitoBean AdminCustomerAssignmentService assignments;
    @MockitoBean UserRepository users;
    @MockitoBean JwtUtil jwtUtil;
    @Test void bindsFiltersAndReturnsExplicitPageMetadata() throws Exception {
        when(browse.search("admin@example.com",true,"김","ACTIVE","JAVA",null,true,1,10))
                .thenReturn(new CustomerPageResponseDto(List.of(),1,10,12,2));
        var auth=new UsernamePasswordAuthenticationToken("admin@example.com",null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(get("/admin/customers").with(authentication(auth)).param("keyword","김").param("status","ACTIVE").param("interestCourse","JAVA").param("unassigned","true").param("page","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray()).andExpect(jsonPath("$.data.number").value(1)).andExpect(jsonPath("$.data.totalPages").value(2));
    }
}
