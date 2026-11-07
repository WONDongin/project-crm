package com.project.crm.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    @Test void issuesAndReadsSignedUserIdentityAndRevocationVersion() {
        var jwt=new JwtUtil();
        ReflectionTestUtils.setField(jwt,"secret","test-only-signing-key-for-crm-jwt-32-bytes-minimum");
        ReflectionTestUtils.setField(jwt,"accessTokenExpiration",3600000L);jwt.init();
        var token=jwt.generateToken("admin@example.com","ROLE_ADMIN","관리자",1L,4L);
        assertThat(jwt.validateToken(token)).isTrue();assertThat(jwt.getUserId(token)).isEqualTo(1L);
        assertThat(jwt.getAuthVersion(token)).isEqualTo(4L);assertThat(jwt.getRole(token)).isEqualTo("ROLE_ADMIN");
    }
}
