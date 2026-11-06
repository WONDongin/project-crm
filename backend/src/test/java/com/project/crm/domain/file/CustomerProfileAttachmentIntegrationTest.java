package com.project.crm.domain.file;

import com.project.crm.domain.activity.*;
import com.project.crm.domain.code.Code;
import com.project.crm.domain.customer.*;
import com.project.crm.domain.customer.dto.UpdateCustomerProfileRequestDto;
import com.project.crm.domain.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.ANY)
@Import({FileService.class, CustomerProfileService.class})
class CustomerProfileAttachmentIntegrationTest {
    @Autowired TestEntityManager em;
    @Autowired FileService files;
    @Autowired FileRepository repository;
    @Autowired CustomerProfileService profiles;
    User owner, other; Customer customer; ActivityLog log, receipt;
    final byte[] pdf = "%PDF-1.7\n%%EOF".getBytes();
    @BeforeEach void setup() {
        owner=user("owner@example.com"); other=user("other@example.com");
        customer=em.persistAndFlush(Customer.builder().name("원래 이름").phone("010-1234-5678")
                .birthDate(LocalDate.of(1997,12,28)).interestCourse("JAVA").leadSource("WEB")
                .status("IN_PROGRESS").lastActivityAt(LocalDateTime.of(2026,11,1,10,0)).counselor(owner).build());
        log=em.persistAndFlush(ActivityLog.builder().customer(customer).counselor(owner).createdBy("CONSULTANT")
                .activityResult("COMPLETED").content("상담 원문").build());
        receipt=em.persistAndFlush(ActivityLog.builder().customer(customer).counselor(owner).createdBy("SYSTEM")
                .activityResult("RECEIVED").content("신청 원문").build());
        var code=new Code();code.setCodeGroup("INTEREST_COURSE");code.setCodeVal("SPRING");code.setUseYn("Y");em.persistAndFlush(code);
    }
    @Test void profileChangesOnlyAllowedFieldsAndPreservesApplication() {
        profiles.update(customer.getCustomerId(), owner.getEmail(), new UpdateCustomerProfileRequestDto("새 이름", "010 9876 5432", "SPRING"));
        em.flush();em.clear();
        var saved=em.find(Customer.class,customer.getCustomerId());
        assertThat(saved.getName()).isEqualTo("새 이름");assertThat(saved.getPhone()).isEqualTo("01098765432");
        assertThat(saved.getInterestCourse()).isEqualTo("SPRING");assertThat(saved.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getCounselor().getUserId()).isEqualTo(owner.getUserId());
        assertThat(saved.getLastActivityAt()).isEqualTo(LocalDateTime.of(2026,11,1,10,0));
        assertThat(em.find(ActivityLog.class,receipt.getActivityId()).getContent()).isEqualTo("신청 원문");
    }
    @Test void profileRejectsNormalizedDuplicateOtherOwnerAndInactiveCode() {
        em.persistAndFlush(Customer.builder().name("중복 고객").phone("010-9876-5432").birthDate(customer.getBirthDate())
                .interestCourse("JAVA").leadSource("WEB").status("NEW").build());
        assertStatus(409, () -> profiles.update(customer.getCustomerId(),owner.getEmail(),new UpdateCustomerProfileRequestDto("이름","01098765432","SPRING")));
        assertStatus(403, () -> profiles.update(customer.getCustomerId(),other.getEmail(),new UpdateCustomerProfileRequestDto("이름","01012345678","SPRING")));
        assertStatus(400, () -> profiles.update(customer.getCustomerId(),owner.getEmail(),new UpdateCustomerProfileRequestDto("이름","01012345678","MISSING")));
        profiles.update(customer.getCustomerId(),owner.getEmail(),new UpdateCustomerProfileRequestDto("이름","01012345678","SPRING")); // 자기 자신 제외
        customer.setCounselor(other);em.flush();em.clear();
        assertStatus(403, () -> profiles.update(customer.getCustomerId(),owner.getEmail(),new UpdateCustomerProfileRequestDto("이름","01012345678","SPRING")));
    }
    @Test void uploadsListsDownloadsAndDeletesBytesWithoutChangingRecord() {
        Long id=files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),upload());
        em.flush();em.clear();
        assertThat(files.list(customer.getCustomerId(),log.getActivityId(),owner.getEmail())).hasSize(1);
        var downloaded=files.download(customer.getCustomerId(),log.getActivityId(),id,owner.getEmail());
        assertThat(downloaded.bytes()).isEqualTo(pdf);assertThat(downloaded.name()).isEqualTo("상담.pdf");
        assertThat(downloaded.type()).isEqualTo("application/pdf");
        assertThat(em.find(ActivityLog.class,log.getActivityId()).getContent()).isEqualTo("상담 원문");
        files.delete(customer.getCustomerId(),log.getActivityId(),id,owner.getEmail());em.flush();em.clear();
        assertThat(repository.findById(id)).isEmpty();
    }
    @Test void reassignmentAllowsNewOwnerToReadButNeitherCanChangeOldAuthorsFiles() {
        Long id=files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),upload());
        customer.setCounselor(other);em.flush();em.clear();
        assertStatus(403,()->files.download(customer.getCustomerId(),log.getActivityId(),id,owner.getEmail()));
        assertThat(files.download(customer.getCustomerId(),log.getActivityId(),id,other.getEmail()).bytes()).isEqualTo(pdf);
        assertStatus(403,()->files.upload(customer.getCustomerId(),log.getActivityId(),other.getEmail(),upload()));
        assertStatus(403,()->files.delete(customer.getCustomerId(),log.getActivityId(),id,other.getEmail()));
        assertStatus(403,()->files.delete(customer.getCustomerId(),log.getActivityId(),id,owner.getEmail()));
    }
    @Test void rejectsSystemWrongCustomerWrongRecordAndInvalidFiles() {
        assertStatus(403,()->files.upload(customer.getCustomerId(),receipt.getActivityId(),owner.getEmail(),upload()));
        assertStatus(403,()->files.list(Long.MAX_VALUE,log.getActivityId(),owner.getEmail()));
        assertStatus(404,()->files.list(customer.getCustomerId(),Long.MAX_VALUE,owner.getEmail()));
        Long id=files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),upload());
        assertStatus(404,()->files.download(customer.getCustomerId(),receipt.getActivityId(),id,owner.getEmail()));
        assertStatus(400,()->files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),new MockMultipartFile("file","fake.png","image/png",pdf)));
        assertStatus(413,()->files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),new MockMultipartFile("file","large.pdf","application/pdf",new byte[10485761])));
        assertThat(repository.count()).isEqualTo(1);
    }
    @Test void enforcesTenFileLimitAndActiveAccount() {
        for(int i=0;i<10;i++)files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),upload());
        assertStatus(409,()->files.upload(customer.getCustomerId(),log.getActivityId(),owner.getEmail(),upload()));
        owner.setStatus("INACTIVE");em.flush();
        assertStatus(403,()->files.list(customer.getCustomerId(),log.getActivityId(),owner.getEmail()));
    }
    private MockMultipartFile upload(){return new MockMultipartFile("file","상담.pdf","application/pdf",pdf);}
    private User user(String email){return em.persistAndFlush(User.builder().email(email).password("hash").name(email).roles("ROLE_CONSULTANT").status("ACTIVE").build());}
    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action){
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(status));
    }
}
