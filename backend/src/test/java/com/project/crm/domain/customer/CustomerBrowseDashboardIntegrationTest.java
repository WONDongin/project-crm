package com.project.crm.domain.customer;

import com.project.crm.domain.code.Code;
import com.project.crm.domain.user.User;
import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.ANY)
@Import({CustomerBrowseService.class,ConsultantDashboardService.class})
class CustomerBrowseDashboardIntegrationTest {
    @Autowired TestEntityManager em;
    @Autowired CustomerBrowseService browse;
    @Autowired ConsultantDashboardService dashboard;
    User admin,owner,other;
    List<Customer> owned=new ArrayList<>();
    @BeforeEach void setup() {
        admin=user("admin@example.com","ROLE_ADMIN");owner=user("owner@example.com","ROLE_CONSULTANT");other=user("other@example.com","ROLE_CONSULTANT");
        for(String value:List.of("NEW","IN_PROGRESS","ACTIVE","DORMANT")) {
            var code=new Code();code.setCodeGroup("CUSTOMER_STATUS");code.setCodeVal(value);code.setCodeNm(value);em.persistAndFlush(code);
        }
        for(int i=0;i<14;i++) owned.add(customer(i==13?"기호%_고객":"김고객 "+i,
                i==0?"010-1111-0000":String.format("010%08d",i),i<10?"IN_PROGRESS":"ACTIVE",i<8?"JAVA":"SPRING",owner,
                i<7?LocalDateTime.of(2026,11,7,10,0).plusMinutes(i):null));
        customer("다른 고객","010-1111-9999","IN_PROGRESS","JAVA",other,LocalDateTime.of(2099,1,1,10,0));
        customer("미배정 고객","01099999999","NEW","JAVA",null,null);
    }
    @Test void pagesOnlyOwnedCustomersWithStableDescendingIdsAndMaskedPhone() {
        var first=search(owner,false,"","","",null,false,0,10);var second=search(owner,false,"","","",null,false,1,10);
        assertThat(first.totalElements()).isEqualTo(14);assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content()).hasSize(10);assertThat(second.content()).hasSize(4);
        assertThat(first.content()).allMatch(row->row.counselorId().equals(owner.getUserId()));
        assertThat(first.content().get(0).customerId()).isEqualTo(owned.get(13).getCustomerId());
        assertThat(first.content()).allMatch(row->row.phone().contains("-***-"));
    }
    @Test void combinesNameStatusCourseAndCounselorFilters() {
        var page=search(admin,true,"김고객 1","IN_PROGRESS","JAVA",owner.getUserId(),false,0,10);
        assertThat(page.content()).extracting(CustomerPageResponseDto.Entry::customerId).containsExactly(owned.get(1).getCustomerId());
        assertThat(search(admin,true,"","","",null,true,0,10).content()).extracting(CustomerPageResponseDto.Entry::name).containsExactly("미배정 고객");
    }
    @Test void normalizesPhoneSearchAndEscapesLiteralLikeWildcards() {
        assertThat(search(owner,false,"010-1111","","",null,false,0,10).totalElements()).isEqualTo(1);
        assertThat(search(admin,true,"0101111","","",null,false,0,10).totalElements()).isEqualTo(2);
        assertThat(search(owner,false,"%_","","",null,false,0,10).content()).extracting(CustomerPageResponseDto.Entry::name).containsExactly("기호%_고객");
    }
    @Test void cannotUseQueryParametersToEscapeConsultantScope() {
        denied(403,()->search(owner,false,"","","",other.getUserId(),false,0,10));
        denied(403,()->search(owner,false,"","","",null,true,0,10));
        denied(403,()->search(admin,false,"","","",null,false,0,10));
    }
    @Test void validatesPagingAndFilterBounds() {
        denied(400,()->search(admin,true,"","","",null,false,-1,10));
        denied(400,()->search(admin,true,"","","",null,false,0,101));
        denied(400,()->search(admin,true,"","","",null,false,Integer.MAX_VALUE,100));
        denied(400,()->search(admin,true,"a".repeat(101),"","",null,false,0,10));
        denied(400,()->search(admin,true,"","","",owner.getUserId(),true,0,10));
    }
    @Test void dashboardCountsIncludeZerosAndRecentFiveExcludeOtherOwnersAndNullDates() {
        var data=dashboard.get(owner.getEmail());assertThat(data.totalCustomers()).isEqualTo(14);
        assertThat(data.statusCounts()).filteredOn(status->status.code().equals("NEW")).extracting(ConsultantDashboardService.Status::count).containsExactly(0L);
        assertThat(data.statusCounts().stream().mapToLong(ConsultantDashboardService.Status::count).sum()).isEqualTo(data.totalCustomers());
        assertThat(data.recentCustomers()).extracting(CustomerPageResponseDto.Entry::customerId).containsExactly(
                owned.get(6).getCustomerId(),owned.get(5).getCustomerId(),owned.get(4).getCustomerId(),owned.get(3).getCustomerId(),owned.get(2).getCustomerId());
    }
    @Test void recentDateTiesUseIdAndRetiredStatusesRemainVisible() {
        owned.get(5).setLastActivityAt(owned.get(6).getLastActivityAt());owned.get(13).setStatus("LEGACY");em.flush();em.clear();
        var data=dashboard.get(owner.getEmail());
        assertThat(data.recentCustomers().get(0).customerId()).isEqualTo(owned.get(6).getCustomerId());
        assertThat(data.statusCounts()).filteredOn(status->status.code().equals("LEGACY")).extracting(ConsultantDashboardService.Status::count).containsExactly(1L);
        assertThat(search(owner,false,"","LEGACY","",null,false,0,10).totalElements()).isEqualTo(1);
    }
    @Test void reassignmentAndEmptyDashboardReflectCurrentOwnership() {
        owned.forEach(customer->customer.setCounselor(other));em.flush();em.clear();
        assertThat(search(owner,false,"","","",null,false,0,10).totalElements()).isZero();
        var data=dashboard.get(owner.getEmail());assertThat(data.totalCustomers()).isZero();assertThat(data.recentCustomers()).isEmpty();
        assertThat(data.statusCounts()).allMatch(status->status.count()==0);
        em.find(User.class,owner.getUserId()).setStatus("INACTIVE");em.flush();
        denied(403,()->dashboard.get(owner.getEmail()));
    }
    private CustomerPageResponseDto search(User actor,boolean admin,String keyword,String status,String course,Long counselor,boolean unassigned,int page,int size) {
        return browse.search(actor.getEmail(),admin,keyword,status,course,counselor,unassigned,page,size);
    }
    private User user(String email,String role) { return em.persistAndFlush(User.builder().email(email).password("hash").name(email).roles(role).specialty("BACKEND").build()); }
    private Customer customer(String name,String phone,String status,String course,User counselor,LocalDateTime date) {
        return em.persistAndFlush(Customer.builder().name(name).phone(phone).birthDate(LocalDate.of(1997,12,28)).interestCourse(course).leadSource("WEB").status(status).counselor(counselor).lastActivityAt(date).build());
    }
    private void denied(int expected,org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(expected));
    }
}
