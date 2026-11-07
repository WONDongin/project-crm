package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLogService;
import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.dto.ConsultApplyRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultApplicationServiceTest {
    @Mock CustomerRepository customers;
    @Mock ActivityLogService activities;
    @Mock CodeRepository codes;
    @InjectMocks CustomerService service;

    @Test void normalizesNewCustomerPhoneAndCreatesOriginalReceipt() {
        when(codes.existsByCodeGroupAndCodeValAndUseYn(anyString(),anyString(),eq("Y"))).thenReturn(true);
        when(customers.findIdentityMatches("01011111111", LocalDate.of(1997,12,28), -1L)).thenReturn(List.of());
        when(customers.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.applyConsult(request());
        var captured = ArgumentCaptor.forClass(Customer.class);
        verify(customers).save(captured.capture());
        assertThat(captured.getValue().getPhone()).isEqualTo("01011111111");
        assertThat(captured.getValue().getBirthDate()).isEqualTo(LocalDate.of(1997,12,28));
        verify(activities).createReceivedLog(captured.getValue(),"PHONE","PM","상담 신청");
    }
    @Test void existingCustomerGetsAnotherReceiptWithoutReplacingProfile() {
        when(codes.existsByCodeGroupAndCodeValAndUseYn(anyString(),anyString(),eq("Y"))).thenReturn(true);
        var existing = Customer.builder().name("기존 이름").phone("01011111111").build();
        when(customers.findIdentityMatches(anyString(),any(),eq(-1L))).thenReturn(List.of(existing));
        service.applyConsult(request());
        verify(customers, never()).save(any());
        verify(activities).createReceivedLog(existing,"PHONE","PM","상담 신청");
        assertThat(existing.getName()).isEqualTo("기존 이름");
    }
    @Test void rejectsInactiveOrMissingApplicationCodesBeforeCustomerWrite() {
        for (String group : List.of("CONSULT_TYPE","INTEREST_COURSE","CONSULT_TIME","LEAD_SOURCE")) {
            reset(codes,customers,activities);
            when(codes.existsByCodeGroupAndCodeValAndUseYn(anyString(),anyString(),eq("Y")))
                    .thenAnswer(invocation -> !group.equals(invocation.getArgument(0)));
            assertThatThrownBy(() -> service.applyConsult(request()))
                    .isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
            verifyNoInteractions(customers,activities);
        }
    }
    private ConsultApplyRequestDto request() {
        var dto = new ConsultApplyRequestDto(); dto.setName("신청자");dto.setPhone("010-1111-1111");
        dto.setBirthDate(LocalDate.of(1997,12,28));dto.setInterestCourse("JAVA");dto.setConsultType("PHONE");
        dto.setPreferredTime("PM");dto.setLeadSource("WEB");dto.setContent("상담 신청");
        return dto;
    }
}
