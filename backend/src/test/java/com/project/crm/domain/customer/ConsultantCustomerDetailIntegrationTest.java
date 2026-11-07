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
                .birthDate(LocalDate.of(1998,1,1)).leadSource("WEB").status("NEW").counselor(owner).build());
        log(anotherCustomer, "SYSTEM", "RECEIVED", "다른 고객 신청", "AM");
        entityManager.clear();

        var application = detailService.getDetail(customer.getCustomerId(), owner.getUserId()).application();
        assertThat(application.activityId()).isEqualTo(latest.getActivityId());
        assertThat(application.content()).isEqualTo("최근 신청");
        assertThat(application.receivedAt()).isEqualTo(receivedAt);
    }

    @Test
    void historyPagesAllRecordsByDateThenIdAndKeepsOriginalData() {
        LocalDateTime date = LocalDateTime.of(2026, 11, 2, 12, 0);
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            ActivityLog record = log(customer, i % 2 == 0 ? "SYSTEM" : "CONSULTANT",
                    i % 2 == 0 ? "RECEIVED" : "COMPLETED", "기록 " + i, "PM");
            record.setCounselor(i % 2 == 0 ? null : owner);
            record.setConsultType("PHONE");
            record.setNextAction("다음 주 연락");
            entityManager.flush();
            setReceivedAt(record, date);
            ids.add(record.getActivityId());
        }
        ActivityLog older = log(customer, "SYSTEM", "RECEIVED", "과거 기록", "AM");
        setReceivedAt(older, date.minusDays(1));
        Customer another = entityManager.persistAndFlush(Customer.builder()
                .name("다른 고객").phone("010-0000-0000").interestCourse("JAVA")
                .birthDate(LocalDate.of(1998,1,1)).leadSource("WEB").status("NEW").counselor(owner).build());
        log(another, "SYSTEM", "RECEIVED", "다른 고객 신청", "AM");
        entityManager.clear();

        var first = detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 0);
        var second = detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 1);
        var third = detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 2);
        assertThat(first.size()).isEqualTo(5);
        assertThat(first.number()).isZero();
        assertThat(first.totalElements()).isEqualTo(12);
        assertThat(first.totalPages()).isEqualTo(3);
        java.util.Collections.reverse(ids);
        assertThat(first.content()).extracting(entry -> entry.activityId()).containsExactlyElementsOf(ids.subList(0, 5));
        assertThat(second.content()).extracting(entry -> entry.activityId()).containsExactlyElementsOf(ids.subList(5, 10));
        assertThat(third.content()).extracting(entry -> entry.activityId()).containsExactly(ids.get(10), older.getActivityId());
        assertThat(first.content().get(0).createdBy()).isEqualTo("SYSTEM");
        assertThat(first.content().get(0).counselorName()).isNull();
        assertThat(first.content().get(1).counselorName()).isEqualTo(owner.getName());
        assertThat(first.content().get(1).consultType()).isEqualTo("PHONE");
        assertThat(first.content().get(1).nextAction()).isEqualTo("다음 주 연락");
        assertThat(first.content().get(0).content()).isEqualTo("기록 10");
        assertThat(first.content().get(0).preferredTime()).isEqualTo("PM");
        assertThat(first.content().get(0).createdAt()).isEqualTo(date);
        assertThat(detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 3).content()).isEmpty();
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Customer.class, customer.getCustomerId()).getLastActivityAt()).isNull();
        assertThat(entityManager.find(ActivityLog.class, ids.get(0)).getContent()).isEqualTo("기록 10");
    }

    @Test
    void emptyHistoryAndInvalidPage() {
        var page = detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 0);
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThatThrownBy(() -> detailService.getHistory(customer.getCustomerId(), owner.getUserId(), -1))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void historyChecksCurrentOwnerAndIncludesPreviousCounselorsRecords() {
        ActivityLog previous = log(customer, "CONSULTANT", "COMPLETED", "이전 상담사 기록", null);
        previous.setCounselor(owner);
        entityManager.flush();
        assertHistoryForbidden(customer.getCustomerId(), other.getUserId());
        assertHistoryForbidden(Long.MAX_VALUE, owner.getUserId());
        customer.setCounselor(other);
        entityManager.flush();
        entityManager.clear();
        assertHistoryForbidden(customer.getCustomerId(), owner.getUserId());
        assertThat(detailService.getHistory(customer.getCustomerId(), other.getUserId(), 0).content())
                .extracting(entry -> entry.activityId()).containsExactly(previous.getActivityId());
        entityManager.find(Customer.class, customer.getCustomerId()).setCounselor(null);
        entityManager.flush();
        entityManager.clear();
        assertHistoryForbidden(customer.getCustomerId(), other.getUserId());
    }


    @Test
    void historyEditableRequiresCurrentAuthorsConsultantRecord() {
        var mine = log(customer, "CONSULTANT", "COMPLETED", "내 기록", null);
        mine.setCounselor(owner);
        var previous = log(customer, "CONSULTANT", "COMPLETED", "이전 담당자 기록", null);
        previous.setCounselor(other);
        var receipt = log(customer, "SYSTEM", "RECEIVED", "신청 원본", null);
        receipt.setCounselor(owner);
        emFlushAndClear();
        var entries = detailService.getHistory(customer.getCustomerId(), owner.getUserId(), 0).content();
        assertThat(entries).filteredOn(e -> e.activityId().equals(mine.getActivityId()))
                .allMatch(e -> e.editable());
        assertThat(entries).filteredOn(e -> !e.activityId().equals(mine.getActivityId()))
                .noneMatch(e -> e.editable());
    }

    private void emFlushAndClear() { entityManager.flush(); entityManager.clear(); }

    private void assertHistoryForbidden(Long customerId, Long counselorId) {
        assertThatThrownBy(() -> detailService.getHistory(customerId, counselorId, 0))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
    }

    private User user(String email) {
        return entityManager.persistAndFlush(User.builder().email(email).password("test-hash")
                .name(email).roles("ROLE_CONSULTANT").specialty("BACKEND").status("ACTIVE").build());
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

