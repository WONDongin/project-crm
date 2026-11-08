package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLog;
import com.project.crm.domain.code.Code;
import com.project.crm.domain.user.User;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.ANY)
@Import(AdminDashboardService.class)
class AdminDashboardIntegrationTest {
    @Autowired TestEntityManager em;
    @Autowired AdminDashboardService dashboard;

    @Test void countsAllCustomersActiveConsultantsAndOnlyTodaysConsultations() {
        var admin = user("admin", "ROLE_ADMIN", "ACTIVE");
        var counselor = user("consultant", "ROLE_CONSULTANT", "ACTIVE");
        user("inactive", "ROLE_CONSULTANT", "INACTIVE");
        var code = new Code(); code.setCodeGroup("CUSTOMER_STATUS"); code.setCodeVal("DORMANT"); code.setCodeNm("휴면"); code.setUseYn("Y"); em.persist(code);
        Customer assigned = customer(1); assigned.setCounselor(counselor); assigned.setStatus("LEGACY");
        for (int i = 2; i <= 7; i++) customer(i);
        var start = LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay();
        activity(assigned, "CONSULTANT", start);
        activity(assigned, "CONSULTANT", start.plusDays(1).minusNanos(1000000));
        activity(assigned, "CONSULTANT", start.minusSeconds(1));
        activity(assigned, "CONSULTANT", start.plusDays(1));
        activity(assigned, "SYSTEM", start.plusHours(1));
        em.flush(); em.clear();
        var result = dashboard.get(admin.getEmail());
        assertThat(result.totalCustomers()).isEqualTo(7);
        assertThat(result.unassignedCustomers()).isEqualTo(6);
        assertThat(result.activeCounselors()).isEqualTo(1);
        assertThat(result.todayConsultations()).isEqualTo(2);
        assertThat(result.statusCounts().stream().mapToLong(AdminDashboardService.Status::count).sum()).isEqualTo(7);
        assertThat(result.statusCounts()).filteredOn(s -> s.code().equals("DORMANT")).extracting(AdminDashboardService.Status::count).containsExactly(0L);
        assertThat(result.statusCounts()).filteredOn(s -> s.code().equals("LEGACY")).extracting(AdminDashboardService.Status::count).containsExactly(1L);
        assertThat(result.recentUnassignedCustomers()).hasSize(5).allMatch(c -> c.counselorId() == null && c.phone().contains("***"));
        assertThat(result.recentUnassignedCustomers()).extracting(c -> c.name()).containsExactly("고객7", "고객6", "고객5", "고객4", "고객3");
    }
    @Test void emptyAndReassignmentReflectCurrentDatabaseAndInactiveAdminsAreDenied() {
        var admin = user("admin", "ROLE_ADMIN", "ACTIVE");
        var counselor = user("consultant", "ROLE_CONSULTANT", "ACTIVE");
        assertThat(dashboard.get(admin.getEmail()).totalCustomers()).isZero();
        assertThat(dashboard.get(admin.getEmail()).recentUnassignedCustomers()).isEmpty();
        var customer = customer(1); em.flush();
        assertThat(dashboard.get(admin.getEmail()).unassignedCustomers()).isEqualTo(1);
        customer.setCounselor(counselor); em.flush();
        assertThat(dashboard.get(admin.getEmail()).unassignedCustomers()).isZero();
        assertThat(dashboard.get(admin.getEmail()).recentUnassignedCustomers()).isEmpty();
        denied(counselor.getEmail(), 403);
        admin.setStatus("INACTIVE"); em.flush(); denied(admin.getEmail(), 403);
        denied("missing@example.com", 401);
    }
    private User user(String name, String role, String status) {
        return em.persistAndFlush(User.builder().email(name+"@example.com").password("hash").name(name).roles(role).specialty("BACKEND").status(status).build());
    }
    private Customer customer(int index) {
        return em.persistAndFlush(Customer.builder().name("고객"+index).phone("0101234000"+index).birthDate(LocalDate.of(1997,12,28)).interestCourse("JAVA").leadSource("WEB").build());
    }
    private void activity(Customer customer, String creator, LocalDateTime date) {
        em.persist(ActivityLog.builder().customer(customer).createdBy(creator).createdAt(date).content("상담").activityResult("COMPLETED").build());
    }
    private void denied(String email, int status) {
        assertThatThrownBy(() -> dashboard.get(email)).isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }
}
