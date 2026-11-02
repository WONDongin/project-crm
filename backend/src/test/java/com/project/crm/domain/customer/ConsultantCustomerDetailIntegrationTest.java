package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLog;
import com.project.crm.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(ConsultantCustomerDetailService.class)
class ConsultantCustomerDetailIntegrationTest {

    @Autowired TestEntityManager entityManager;
    @Autowired ConsultantCustomerDetailService detailService;

    private User owner;
    private User other;
    private Customer customer;

    @BeforeEach
    void setUp() {
        owner = user("owner@example.com");
        other = user("other@example.com");
        customer = entityManager.persistAndFlush(Customer.builder()
                .name("김고객").phone("010-1234-5678")
                .birthDate(LocalDate.of(1997, 12, 28)).email("customer@example.com")
                .interestCourse("JAVA").leadSource("WEB").status("IN_PROGRESS")
                .counselor(owner).build());
    }

    @Test
    void returnsAssignedCustomerAndOriginalApplicationWithoutChangingEither() {
        ActivityLog application = log(customer, "SYSTEM", "RECEIVED", "자바 과정 문의\n주말 상담 희망", "PM");
        entityManager.clear();

        var response = detailService.getDetail(customer.getCustomerId(), owner.getUserId());

        assertThat(response.name()).isEqualTo("김고객");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1997, 12, 28));
        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.interestCourse()).isEqualTo("JAVA");
        assertThat(response.lastActivityAt()).isNull();
        assertThat(response.application().activityId()).isEqualTo(application.getActivityId());
        assertThat(response.application().content()).isEqualTo("자바 과정 문의\n주말 상담 희망");
        assertThat(response.application().preferredTime()).isEqualTo("PM");
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Customer.class, customer.getCustomerId()).getStatus())
                .isEqualTo("IN_PROGRESS");
        assertThat(entityManager.find(ActivityLog.class, application.getActivityId()).getContent())
                .isEqualTo("자바 과정 문의\n주말 상담 희망");
    }

    @Test
    void rejectsAnotherConsultantsCustomer() {
        assertForbidden(customer.getCustomerId(), other.getUserId());
    }

    @Test
    void rejectsUnassignedCustomer() {
        customer.setCounselor(null);
        entityManager.flush();
        entityManager.clear();
        assertForbidden(customer.getCustomerId(), owner.getUserId());
    }

    @Test
    void rejectsMissingCustomerWithoutDisclosingExistence() {
        assertForbidden(Long.MAX_VALUE, owner.getUserId());
    }

    @Test
    void appliesCurrentAssignmentAfterReassignment() {
        customer.setCounselor(other);
        entityManager.flush();
        entityManager.clear();
        assertForbidden(customer.getCustomerId(), owner.getUserId());
        assertThat(detailService.getDetail(customer.getCustomerId(), other.getUserId()).customerId())
                .isEqualTo(customer.getCustomerId());
    }

    @Test
    void returnsNullApplicationWhenThereIsNoSystemReceipt() {
        log(customer, "CONSULTANT", "RECEIVED", "상담사가 남긴 기록", null);
        log(customer, "SYSTEM", "COMPLETED", "신청 접수가 아닌 시스템 기록", null);
        entityManager.clear();
        assertThat(detailService.getDetail(customer.getCustomerId(), owner.getUserId()).application()).isNull();
    }

    @Test
    void choosesLatestReceiptByDateThenIdAndIgnoresOtherRecords() {
        ActivityLog old = log(customer, "SYSTEM", "RECEIVED", "이전 신청", "AM");
        ActivityLog latest = log(customer, "SYSTEM", "RECEIVED", "최근 신청", "PM");
        ActivityLog laterIdButOlderDate = log(customer, "SYSTEM", "RECEIVED", "과거 접수", "AM");
        LocalDateTime receivedAt = LocalDateTime.of(2026, 11, 2, 10, 0);
        setReceivedAt(old, receivedAt);
        setReceivedAt(latest, receivedAt);
        setReceivedAt(laterIdButOlderDate, receivedAt.minusDays(1));
        log(customer, "CONSULTANT", "RECEIVED", "일반 상담", null);
        log(customer, "SYSTEM", "COMPLETED", "시스템 처리", null);
        Customer anotherCustomer = entityManager.persistAndFlush(Customer.builder()
                .name("다른 고객").phone("010-9999-9999").interestCourse("JAVA")
                .leadSource("WEB").status("NEW").counselor(owner).build());
        log(anotherCustomer, "SYSTEM", "RECEIVED", "다른 고객 신청", "AM");
        entityManager.clear();

        var application = detailService.getDetail(customer.getCustomerId(), owner.getUserId()).application();
        assertThat(application.activityId()).isEqualTo(latest.getActivityId());
        assertThat(application.content()).isEqualTo("최근 신청");
        assertThat(application.receivedAt()).isEqualTo(receivedAt);
    }

    private User user(String email) {
        return entityManager.persistAndFlush(User.builder().email(email).password("test-hash")
                .name(email).roles("ROLE_CONSULTANT").status("ACTIVE").build());
    }

    private ActivityLog log(Customer target, String createdBy, String result, String content, String time) {
        return entityManager.persistAndFlush(ActivityLog.builder().customer(target).createdBy(createdBy)
                .activityResult(result).content(content).preferredTime(time).build());
    }

    private void setReceivedAt(ActivityLog log, LocalDateTime time) {
        entityManager.getEntityManager().createNativeQuery(
                        "update activity_logs set created_at = :time where activity_id = :id")
                .setParameter("time", time).setParameter("id", log.getActivityId()).executeUpdate();
    }

    private void assertForbidden(Long customerId, Long counselorId) {
        assertThatThrownBy(() -> detailService.getDetail(customerId, counselorId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
    }
}
