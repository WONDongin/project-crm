package com.project.crm.domain.customer;
// 고객 비즈니스 로직

import com.project.crm.domain.activity.ActivityLogService;
import com.project.crm.domain.customer.dto.ConsultApplyRequestDto;
import com.project.crm.domain.customer.dto.ConsultantCustomerResponseDto;
import com.project.crm.domain.customer.dto.CustomerResponseDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

        private final CustomerRepository customerRepository;
        private final ActivityLogService activityLogService;

        // 전체 고객 조회
        public List<CustomerResponseDto> getCustomers() {

                return customerRepository.findAll()
                        .stream()
                        .map(customer -> CustomerResponseDto.builder()
                                .customerId(customer.getCustomerId())
                                .name(customer.getName())
                                .phone(customer.getPhone())
                                .interestCourse(customer.getInterestCourse())
                                .status(customer.getStatus())

                                .counselorId(
                                        customer.getCounselor() != null
                                                ? customer.getCounselor().getUserId()
                                                : null
                                )

                                .counselorName(
                                        customer.getCounselor() != null
                                                ? customer.getCounselor().getName()
                                                : "미지정"
                                )

                                .build())
                        .toList();
        }

        // 상담사별 고객 조회
        public List<ConsultantCustomerResponseDto> getConsultantCustomers(Long userId) {

                return customerRepository.findByCounselor_UserId(userId)
                        .stream()
                        .map(customer -> ConsultantCustomerResponseDto.builder()

                                .customerId(customer.getCustomerId())
                                .name(customer.getName())
                                .phone(customer.getPhone())
                                .interestCourse(customer.getInterestCourse())
                                .status(customer.getStatus())
                                .customerTag(customer.getCustomerTag())
                                .counselorId(
                                        customer.getCounselor() != null
                                                ? customer.getCounselor().getUserId()
                                                : null
                                )

                                .counselorName(
                                        customer.getCounselor() != null
                                                ? customer.getCounselor().getName()
                                                : "미지정"
                                )

                                .lastActivityAt(customer.getLastActivityAt())

                                .leadSource(customer.getLeadSource())

                                .build())

                        .toList();
        }

        // 상담 신청
        @Transactional
        public void applyConsult(ConsultApplyRequestDto request) {

        Customer customer =
                customerRepository
                        .findByPhoneAndBirthDate(
                                request.getPhone(),
                                request.getBirthDate()
                        )
                        .orElseGet(() -> {

                                Customer newCustomer = Customer.builder()
                                        .name(request.getName())
                                        .phone(request.getPhone())
                                        .birthDate(request.getBirthDate())
                                        .email(request.getEmail())
                                        .interestCourse(request.getInterestCourse())
                                        .leadSource(request.getLeadSource())
                                        .status("NEW")
                                        .build();

                                return customerRepository.save(newCustomer);
                        });

        activityLogService.createReceivedLog(
                customer,
                request.getContent()
        );
}
}