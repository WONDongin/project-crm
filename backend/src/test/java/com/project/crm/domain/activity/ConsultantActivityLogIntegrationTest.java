package com.project.crm.domain.activity;

import com.project.crm.domain.activity.dto.CreateActivityLogRequestDto;
import com.project.crm.domain.code.Code;
import com.project.crm.domain.customer.Customer;
import com.project.crm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(ConsultantActivityLogService.class)
class ConsultantActivityLogIntegrationTest {
    @Autowired TestEntityManager em;
    @Autowired ConsultantActivityLogService service;
    @Autowired ActivityLogRepository logs;
    User owner;
    User other;
    Customer customer;
    CreateActivityLogRequestDto request = new CreateActivityLogRequestDto("PHONE", "COMPLETED", " 상담 내용 ", " 다음 주 연락 ");

    @BeforeEach void setup() {
        owner = user("owner@example.com"); other = user("other@example.com");
        customer = em.persistAndFlush(Customer.builder().name("김고객").phone("010-1234-5678")
                .interestCourse("JAVA").leadSource("WEB").status("IN_PROGRESS").counselor(owner).build());
        code("CONSULT_TYPE", "PHONE", "Y"); code("ACTIVITY_RESULT", "COMPLETED", "Y");
        code("ACTIVITY_RESULT", "RECEIVED", "Y"); code("CONSULT_TYPE", "INACTIVE", "N");
    }
    @Test void savesAuthorAndAllFieldsAndUpdatesLastDateWithoutChangingReceipt() {
        var receipt = em.persistAndFlush(ActivityLog.builder().customer(customer).createdBy("SYSTEM")
                .activityResult("RECEIVED").content("신청 원본").build());
        Long id = service.create(customer.getCustomerId(), owner.getEmail(), request);
        em.flush(); em.clear();
        var saved = em.find(ActivityLog.class, id);
        assertThat(saved.getCounselor().getUserId()).isEqualTo(owner.getUserId());
        assertThat(saved.getCreatedBy()).isEqualTo("CONSULTANT");
        assertThat(saved.getConsultType()).isEqualTo("PHONE");
        assertThat(saved.getActivityResult()).isEqualTo("COMPLETED");
        assertThat(saved.getContent()).isEqualTo("상담 내용");
        assertThat(saved.getNextAction()).isEqualTo("다음 주 연락");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(em.find(Customer.class, customer.getCustomerId()).getLastActivityAt()).isEqualTo(saved.getCreatedAt());
        assertThat(em.find(ActivityLog.class, receipt.getActivityId()).getContent()).isEqualTo("신청 원본");
    }
    @Test void deniesAnotherOwnerMissingAndUnassignedCustomers() {
        denied(customer.getCustomerId(), other.getEmail(), 403);
        denied(Long.MAX_VALUE, owner.getEmail(), 403);
        customer.setCounselor(null); em.flush(); em.clear();
        denied(customer.getCustomerId(), owner.getEmail(), 403);
        assertThat(logs.count()).isZero();
    }
    @Test void rechecksAssignmentAndKeepsPreviousAuthor() {
        Long first = service.create(customer.getCustomerId(), owner.getEmail(), request);
        customer.setCounselor(other); em.flush(); em.clear();
        denied(customer.getCustomerId(), owner.getEmail(), 403);
        Long second = service.create(customer.getCustomerId(), other.getEmail(), request);
        em.flush(); em.clear();
        assertThat(em.find(ActivityLog.class, first).getCounselor().getUserId()).isEqualTo(owner.getUserId());
        assertThat(em.find(ActivityLog.class, second).getCounselor().getUserId()).isEqualTo(other.getUserId());
    }
    @Test void rejectsReservedUnknownAndDisabledCodesWithoutWrites() {
        for (var invalid : java.util.List.of(
                new CreateActivityLogRequestDto("PHONE", "RECEIVED", "내용", null),
                new CreateActivityLogRequestDto("UNKNOWN", "COMPLETED", "내용", null),
                new CreateActivityLogRequestDto("INACTIVE", "COMPLETED", "내용", null),
                new CreateActivityLogRequestDto("PHONE", "UNKNOWN", "내용", null))) {
            assertThatThrownBy(() -> service.create(customer.getCustomerId(), owner.getEmail(), invalid))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
        assertThat(logs.count()).isZero(); assertThat(customer.getLastActivityAt()).isNull();
    }
    @Test void blocksDeletedInactiveOrNonConsultantAuthor() {
        denied(customer.getCustomerId(), "missing@example.com", 401);
        owner.setStatus("INACTIVE"); em.flush(); denied(customer.getCustomerId(), owner.getEmail(), 403);
        owner.setStatus("ACTIVE"); owner.setRoles("ROLE_ADMIN"); em.flush();
        denied(customer.getCustomerId(), owner.getEmail(), 403);
        assertThat(logs.count()).isZero();
    }
    private void denied(Long id, String email, int status) {
        assertThatThrownBy(() -> service.create(id, email, request)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }
    private User user(String email) {
        return em.persistAndFlush(User.builder().email(email).password("hash").name(email)
                .roles("ROLE_CONSULTANT").status("ACTIVE").build());
    }
    private void code(String group, String value, String active) {
        Code c = new Code(); c.setCodeGroup(group); c.setCodeVal(value); c.setUseYn(active); em.persistAndFlush(c);
    }
}
