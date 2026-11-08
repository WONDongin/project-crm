package com.project.crm.domain.user;

import com.project.crm.domain.activity.*;
import com.project.crm.domain.code.Code;
import com.project.crm.domain.customer.*;
import com.project.crm.domain.user.dto.AdminUserRequestDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.ANY)
@Import({AdminUserService.class,AdminCustomerAssignmentService.class,AdminUserManagementIntegrationTest.Passwords.class})
class AdminUserManagementIntegrationTest {
    @TestConfiguration static class Passwords { @Bean BCryptPasswordEncoder passwords() { return new BCryptPasswordEncoder(); } }
    @Autowired TestEntityManager em;
    @Autowired AdminUserService service;
    @Autowired AdminCustomerAssignmentService assignments;
    @Autowired BCryptPasswordEncoder passwords;
    User admin, consultant, replacement;
    Customer customer;
    ActivityLog original;
    @BeforeEach void setup() {
        admin=user("admin@example.com","ROLE_ADMIN");consultant=user("consultant@example.com","ROLE_CONSULTANT");replacement=user("next@example.com","ROLE_CONSULTANT");
        for(String value:new String[]{"ROLE_ADMIN","ROLE_CONSULTANT"})code("USER_ROLE",value);
        code("USER_SPECIALTY","BACKEND");code("USER_STATUS","ACTIVE");code("USER_STATUS","INACTIVE");
        customer=em.persistAndFlush(Customer.builder().name("고객").phone("01011111111").birthDate(LocalDate.of(1997,12,28))
                .interestCourse("JAVA").leadSource("WEB").status("IN_PROGRESS").counselor(consultant).lastActivityAt(LocalDateTime.of(2026,11,1,10,0)).build());
        original=em.persistAndFlush(ActivityLog.builder().customer(customer).counselor(consultant).content("신청 원본").fileUploadNo(42L).build());
    }
    @Test void createsEncryptedAccountAndRejectsDuplicateEmailIgnoringCase() {
        var created=service.create(admin.getEmail(),new AdminUserRequestDto.Create("NEW@EXAMPLE.COM","새 상담사","newpass123!","ROLE_CONSULTANT","BACKEND","ACTIVE",null));
        var saved=em.find(User.class,created.getUserId());assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPassword()).isNotEqualTo("newpass123!");assertThat(passwords.matches("newpass123!",saved.getPassword())).isTrue();
        assertStatus(409,()->service.create(admin.getEmail(),new AdminUserRequestDto.Create("NEW@example.com","중복","newpass123!","ROLE_CONSULTANT","BACKEND","ACTIVE",null)));
    }
    @Test void deactivationReleasesCustomersButPreservesStatusHistoryAndFileGroup() {
        var result=service.update(consultant.getUserId(),admin.getEmail(),update("ROLE_CONSULTANT","INACTIVE"));
        assertThat(result.releasedCustomerCount()).isEqualTo(1);em.flush();em.clear();
        var saved=em.find(Customer.class,customer.getCustomerId());assertThat(saved.getCounselor()).isNull();assertThat(saved.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getLastActivityAt()).isEqualTo(LocalDateTime.of(2026,11,1,10,0));
        var log=em.find(ActivityLog.class,original.getActivityId());assertThat(log.getCounselor().getUserId()).isEqualTo(consultant.getUserId());
        assertThat(log.getContent()).isEqualTo("신청 원본");assertThat(log.getFileUploadNo()).isEqualTo(42L);
        assertThat(em.find(User.class,consultant.getUserId()).getAuthVersion()).isEqualTo(1);
        assertStatus(400,()->assignments.assignCounselor(customer.getCustomerId(),consultant.getUserId(),admin.getEmail()));
    }
    @Test void roleChangeReleasesAssignmentsAndReactivationDoesNotRestoreOldTokenVersion() {
        service.update(consultant.getUserId(),admin.getEmail(),update("ROLE_ADMIN","ACTIVE"));
        assertThat(customer.getCounselor()).isNull();assertThat(consultant.getAuthVersion()).isEqualTo(1);
        service.update(consultant.getUserId(),admin.getEmail(),update("ROLE_CONSULTANT","ACTIVE"));
        assertThat(consultant.getAuthVersion()).isEqualTo(2);assertThat(customer.getCounselor()).isNull();
    }
    @Test void rejectsSelfDemotionAndDuplicateEmailWithoutMutatingData() {
        assertStatus(409,()->service.update(admin.getUserId(),admin.getEmail(),new AdminUserRequestDto.Update(admin.getEmail(),"관리자","ROLE_CONSULTANT","BACKEND","ACTIVE",null)));
        assertStatus(409,()->service.update(consultant.getUserId(),admin.getEmail(),new AdminUserRequestDto.Update(admin.getEmail(),"중복","ROLE_CONSULTANT","BACKEND","ACTIVE",null)));
        assertThat(admin.getRoles()).isEqualTo("ROLE_ADMIN");assertThat(customer.getCounselor()).isEqualTo(consultant);
    }
    @Test void passwordResetRevokesTokensAndDoesNotReleaseCustomers() {
        var result=service.resetPassword(consultant.getUserId(),admin.getEmail(),"resetpass123!");
        assertThat(result.user().getAssignedCustomerCount()).isEqualTo(1);em.flush();em.clear();
        var saved=em.find(User.class,consultant.getUserId());assertThat(saved.getAuthVersion()).isEqualTo(1);
        assertThat(passwords.matches("resetpass123!",saved.getPassword())).isTrue();
        assertThat(em.find(Customer.class,customer.getCustomerId()).getCounselor().getUserId()).isEqualTo(consultant.getUserId());
    }
    @Test void explicitUnassignmentPreservesHistoryAndReassignmentUsesActiveConsultant() {
        assignments.unassignCounselor(customer.getCustomerId(),admin.getEmail());assertThat(customer.getCounselor()).isNull();
        assertThat(customer.getStatus()).isEqualTo("IN_PROGRESS");
        assignments.assignCounselor(customer.getCustomerId(),replacement.getUserId(),admin.getEmail());
        assertThat(customer.getCounselor()).isEqualTo(replacement);assertThat(original.getCounselor()).isEqualTo(consultant);
        assertStatus(400,()->assignments.assignCounselor(customer.getCustomerId(),admin.getUserId(),admin.getEmail()));
    }
    @Test void rejectsConsultantAndInactiveAdminForMutations() {
        assertStatus(403,()->assignments.unassignCounselor(customer.getCustomerId(),consultant.getEmail()));
        admin.setStatus("INACTIVE");em.flush();
        assertStatus(403,()->service.resetPassword(consultant.getUserId(),admin.getEmail(),"resetpass123!"));
    }
    @Test void listIncludesCurrentAssignedCustomerCount() {
        assertThat(service.list(admin.getEmail(), "", "", "", 0, 10).content()).filteredOn(u->u.getUserId().equals(consultant.getUserId()))
                .extracting(u->u.getAssignedCustomerCount()).containsExactly(1L);
    }
    @Test void pagesUsersOnServerWithStableNewestFirstOrderAndAssignmentCounts() {
        var newest = replacement;
        for (int i = 0; i < 20; i++) newest = user("person" + i + "@example.com", "ROLE_CONSULTANT");
        var first = service.list(admin.getEmail(), "", "", "", 0, 10);
        var last = service.list(admin.getEmail(), "", "", "", 2, 10);
        assertThat(first.totalElements()).isEqualTo(23);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.content()).hasSize(10);
        assertThat(first.content().get(0).getUserId()).isEqualTo(newest.getUserId());
        assertThat(last.content()).extracting(u -> u.getUserId()).containsExactly(
                replacement.getUserId(), consultant.getUserId(), admin.getUserId());
        assertThat(last.content()).filteredOn(u -> u.getUserId().equals(consultant.getUserId()))
                .extracting(u -> u.getAssignedCustomerCount()).containsExactly(1L);
        assertThat(first.content()).allMatch(u -> u.getAssignedCustomerCount() == 0);
    }
    @Test void combinesNameOrEmailWithRoleAndStatusAndIgnoresCaseAndOuterWhitespace() {
        consultant.setName("김상담"); consultant.setStatus("INACTIVE"); em.flush();
        var byName = service.list(admin.getEmail(), "  김상담  ", " ROLE_CONSULTANT ", "INACTIVE", 0, 10);
        assertThat(byName.content()).extracting(u -> u.getUserId()).containsExactly(consultant.getUserId());
        assertThat(service.list(admin.getEmail(), "CONSULTANT@EXAMPLE", "ROLE_CONSULTANT", "", 0, 10).totalElements()).isEqualTo(1);
        assertThat(service.list(admin.getEmail(), "김상담", "ROLE_ADMIN", "", 0, 10).totalElements()).isZero();
        assertThat(service.list(admin.getEmail(), "김상담", "", "ACTIVE", 0, 10).totalElements()).isZero();
    }
    @Test void treatsSqlWildcardsAsLiteralSearchTextAndReturnsEmptyPagesSafely() {
        consultant.setName("상담%_!기호"); em.flush();
        assertThat(service.list(admin.getEmail(), "%_!", "", "", 0, 10).content())
                .extracting(u -> u.getUserId()).containsExactly(consultant.getUserId());
        var absent = service.list(admin.getEmail(), "없는사용자", "", "", 0, 10);
        assertThat(absent.content()).isEmpty(); assertThat(absent.totalPages()).isZero();
        var beyond = service.list(admin.getEmail(), "", "", "", 20, 10);
        assertThat(beyond.content()).isEmpty(); assertThat(beyond.totalElements()).isEqualTo(3);
    }
    @Test void updatedAccountFallsOutOfCurrentFilterAndTotalsAreRecomputed() {
        assertThat(service.list(admin.getEmail(), "", "ROLE_CONSULTANT", "ACTIVE", 0, 10).totalElements()).isEqualTo(2);
        service.update(consultant.getUserId(), admin.getEmail(), update("ROLE_CONSULTANT", "INACTIVE"));
        var active = service.list(admin.getEmail(), "", "ROLE_CONSULTANT", "ACTIVE", 0, 10);
        assertThat(active.totalElements()).isEqualTo(1);
        assertThat(active.content()).extracting(u -> u.getUserId()).containsExactly(replacement.getUserId());
    }
    @Test void validatesSearchBoundsAndRequiresAnActiveAdmin() {
        for (int[] range : new int[][]{{-1,10},{0,0},{0,101},{Integer.MAX_VALUE,100}}) {
            assertStatus(400, () -> service.list(admin.getEmail(), "", "", "", range[0], range[1]));
        }
        assertStatus(400, () -> service.list(admin.getEmail(), "a".repeat(101), "", "", 0, 10));
        assertStatus(400, () -> service.list(admin.getEmail(), "", "a".repeat(21), "", 0, 10));
        assertStatus(400, () -> service.list(admin.getEmail(), "", "", "a".repeat(21), 0, 10));
        assertStatus(403, () -> service.list(consultant.getEmail(), "", "", "", 0, 10));
        assertStatus(401, () -> service.list("missing@example.com", "", "", "", 0, 10));
        admin.setStatus("INACTIVE"); em.flush();
        assertStatus(403, () -> service.list(admin.getEmail(), "", "", "", 0, 10));
    }
    private AdminUserRequestDto.Update update(String role,String status) {
        return new AdminUserRequestDto.Update(consultant.getEmail(),"상담사",role,"BACKEND",status,"메모");
    }
    private User user(String email,String role) { return em.persistAndFlush(User.builder().email(email).password("hash").name(email).roles(role).specialty("BACKEND").build()); }
    private void code(String group,String value) { var c=new Code();c.setCodeGroup(group);c.setCodeVal(value);c.setCodeNm(value);em.persistAndFlush(c); }
    private void assertStatus(int expected,org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(expected));
    }
}
