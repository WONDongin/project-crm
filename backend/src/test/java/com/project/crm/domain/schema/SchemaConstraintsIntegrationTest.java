package com.project.crm.domain.schema;

import com.project.crm.domain.code.*;
import com.project.crm.domain.activity.*;
import com.project.crm.domain.file.*;
import com.project.crm.domain.customer.*;
import com.project.crm.domain.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SchemaConstraintsIntegrationTest {
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired CodeRepository codes;
    @Autowired TestEntityManager em;
    @Autowired ActivityLogRepository activities;
    @Autowired FileRepository files;

    @Test void customerIdentityRejectsSamePhoneAndBirthDate() {
        customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28)));
        assertThatThrownBy(() -> customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void samePhoneWithDifferentBirthDatesAndSameBirthDateWithDifferentPhonesAreAllowed() {
        customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28)));
        customers.saveAndFlush(customer("01011111111", LocalDate.of(1998,1,1)));
        customers.saveAndFlush(customer("01022222222", LocalDate.of(1997,12,28)));
        assertThat(customers.count()).isEqualTo(3);
    }
    @Test void customerBirthDateCannotBeMissing() {
        assertThatThrownBy(() -> customers.saveAndFlush(customer("01011111111", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void specialtyCannotBeMissing() {
        assertThatThrownBy(() -> users.saveAndFlush(User.builder().email("owner@example.com")
                .password("hash").name("상담사").roles("ROLE_CONSULTANT").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void codeNameCannotBeMissing() {
        var code = new Code(); code.setCodeGroup("TEST"); code.setCodeVal("MISSING_NAME");
        assertThatThrownBy(() -> codes.saveAndFlush(code)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void applicationInitializesCodeDefaultsInsteadOfSendingNullToDatabase() {
        var code = new Code(); code.setCodeGroup("TEST"); code.setCodeVal("DEFAULTS"); code.setCodeNm("기본값");
        code.setDepth(null); code.setUseYn(null);
        codes.saveAndFlush(code); em.clear();
        var saved = codes.findById(new CodeId("TEST", "DEFAULTS")).orElseThrow();
        assertThat(saved.getDepth()).isZero(); assertThat(saved.getUseYn()).isEqualTo("Y");
        assertThat(saved.getCreatedAt()).isNotNull(); assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }
    @Test void explicitCodeSettingsArePreservedAndStatusDefaultsMatchDatabase() {
        var code = new Code(); code.setCodeGroup("TEST"); code.setCodeVal("DISABLED"); code.setCodeNm("비활성");
        code.setDepth(2); code.setUseYn("N");
        var saved = codes.saveAndFlush(code);
        assertThat(saved.getDepth()).isEqualTo(2); assertThat(saved.getUseYn()).isEqualTo("N");
        var customer = customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28)));
        assertThat(customer.getStatus()).isEqualTo("NEW");
        var user = users.saveAndFlush(User.builder().email("owner@example.com").password("hash")
                .name("상담사").roles("ROLE_CONSULTANT").specialty("BACKEND").build());
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
    }
    @Test void newActivityAndFileHaveRequiredApplicationDefaults() {
        var customer = customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28)));
        var activity = activities.saveAndFlush(ActivityLog.builder().customer(customer).content("신청 내용").build());
        assertThat(activity.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(activity.getActivityResult()).isEqualTo("RECEIVED");
        assertThat(activity.getCreatedAt()).isNotNull();
        var file = files.saveAndFlush(FileMeta.builder().fileUploadNo(1L).fileCategory("activity_logs")
                .originalName("상담.pdf").savedName("saved").filePath(".").sizeBytes(5).build());
        assertThat(file.getDelYn()).isEqualTo("N"); assertThat(file.getUploadedAt()).isNotNull();
    }
    @Test void activityContentCannotBeMissing() {
        var customer = customers.saveAndFlush(customer("01011111111", LocalDate.of(1997,12,28)));
        assertThatThrownBy(() -> activities.saveAndFlush(ActivityLog.builder().customer(customer).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    private Customer customer(String phone, LocalDate birthDate) {
        return Customer.builder().name("고객").phone(phone).birthDate(birthDate)
                .interestCourse("JAVA").leadSource("WEB").build();
    }
}
