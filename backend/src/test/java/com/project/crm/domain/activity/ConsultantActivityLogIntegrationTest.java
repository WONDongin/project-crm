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
                .birthDate(java.time.LocalDate.of(1997,12,28)).interestCourse("JAVA").leadSource("WEB").status("IN_PROGRESS").counselor(owner).build());
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

    @Test void updatesOwnRecordAndPreservesAuthorCreationDateAndLastActivity() {
        Long id = service.create(customer.getCustomerId(), owner.getEmail(), request);
        em.flush(); em.clear();
        var before = em.find(ActivityLog.class, id);
        var createdAt = before.getCreatedAt();
        var lastActivityAt = em.find(Customer.class, customer.getCustomerId()).getLastActivityAt();
        service.update(customer.getCustomerId(), id, owner.getEmail(),
                new CreateActivityLogRequestDto("PHONE", "COMPLETED", " 수정 내용 ", null));
        em.flush(); em.clear();
        var updated = em.find(ActivityLog.class, id);
        assertThat(updated.getContent()).isEqualTo("수정 내용");
        assertThat(updated.getNextAction()).isNull();
        assertThat(updated.getConsultType()).isEqualTo("PHONE");
        assertThat(updated.getActivityResult()).isEqualTo("COMPLETED");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getCounselor().getUserId()).isEqualTo(owner.getUserId());
        assertThat(updated.getCreatedBy()).isEqualTo("CONSULTANT");
        assertThat(em.find(Customer.class, customer.getCustomerId()).getLastActivityAt()).isEqualTo(lastActivityAt);
    }

    @Test void rejectsSystemOriginalEvenWhenAssignedToCurrentCounselor() {
        var receipt = em.persistAndFlush(ActivityLog.builder().customer(customer).counselor(owner)
                .createdBy("SYSTEM").activityResult("RECEIVED").content("신청 원본").build());
        updateDenied(customer.getCustomerId(), receipt.getActivityId(), owner.getEmail(), 403);
        assertThat(receipt.getContent()).isEqualTo("신청 원본");
    }

    @Test void reassignmentBlocksOldOwnerAndNewOwnerCannotEditPreviousAuthorsRecord() {
        Long id = service.create(customer.getCustomerId(), owner.getEmail(), request);
        customer.setCounselor(other); em.flush(); em.clear();
        updateDenied(customer.getCustomerId(), id, owner.getEmail(), 403);
        updateDenied(customer.getCustomerId(), id, other.getEmail(), 403);
        assertThat(em.find(ActivityLog.class, id).getContent()).isEqualTo("상담 내용");
    }

    @Test void rejectsMismatchedCustomerAndMissingRecord() {
        Long id = service.create(customer.getCustomerId(), owner.getEmail(), request);
        var another = em.persistAndFlush(Customer.builder().name("다른 고객").phone("010-9999-9999")
                .birthDate(java.time.LocalDate.of(1997,12,28)).interestCourse("JAVA").leadSource("WEB").status("IN_PROGRESS").counselor(owner).build());
        updateDenied(another.getCustomerId(), id, owner.getEmail(), 404);
        updateDenied(customer.getCustomerId(), Long.MAX_VALUE, owner.getEmail(), 404);
        customer.setCounselor(null); em.flush(); em.clear();
        updateDenied(customer.getCustomerId(), id, owner.getEmail(), 403);
    }

    @Test void rejectsInactiveAuthorAndInvalidCodesBeforeMutatingRecord() {
        Long id = service.create(customer.getCustomerId(), owner.getEmail(), request);
        updateDenied(customer.getCustomerId(), id, "missing@example.com", 401);
        owner.setStatus("INACTIVE"); em.flush();
        updateDenied(customer.getCustomerId(), id, owner.getEmail(), 403);
        owner.setStatus("ACTIVE"); owner.setRoles("ROLE_ADMIN"); em.flush();
        updateDenied(customer.getCustomerId(), id, owner.getEmail(), 403);
        owner.setRoles("ROLE_CONSULTANT"); em.flush();
        for (var invalid : java.util.List.of(
                new CreateActivityLogRequestDto("PHONE", "RECEIVED", "변경", null),
                new CreateActivityLogRequestDto("INACTIVE", "COMPLETED", "변경", null),
                new CreateActivityLogRequestDto("UNKNOWN", "COMPLETED", "변경", null))) {
            assertThatThrownBy(() -> service.update(customer.getCustomerId(), id, owner.getEmail(), invalid))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
        assertThat(em.find(ActivityLog.class, id).getContent()).isEqualTo("상담 내용");
    }

    private void updateDenied(Long customerId, Long activityId, String email, int status) {
        assertThatThrownBy(() -> service.update(customerId, activityId, email, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }

    private void denied(Long id, String email, int status) {
        assertThatThrownBy(() -> service.create(id, email, request)).isInstanceOfSatisfying(
                ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }
    private User user(String email) {
        return em.persistAndFlush(User.builder().email(email).password("hash").name(email)
                .roles("ROLE_CONSULTANT").specialty("BACKEND").status("ACTIVE").build());
    }
    private void code(String group, String value, String active) {
        Code c = new Code(); c.setCodeGroup(group); c.setCodeVal(value); c.setCodeNm(value); c.setDepth(0); c.setUseYn(active); c.setCreatedAt(java.time.LocalDateTime.now().withNano(0)); em.persistAndFlush(c);
    }
}
