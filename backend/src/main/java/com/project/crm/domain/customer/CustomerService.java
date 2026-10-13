package com.project.crm.domain.customer;
// 고객 비즈니스 로직

import com.project.crm.domain.activity.ActivityLogService;
import com.project.crm.domain.code.CodeRepository;
import com.project.crm.common.util.PhoneNumberMasker;
import com.project.crm.domain.customer.dto.ConsultApplyRequestDto;
import com.project.crm.domain.customer.dto.ConsultantCustomerResponseDto;
import com.project.crm.domain.customer.dto.CustomerResponseDto;
import com.project.crm.domain.customer.dto.UpdateConsultantCustomerRequestDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

        private final CustomerRepository customerRepository;
        private final ActivityLogService activityLogService;
        private final CodeRepository codeRepository;

        // 전체 고객 조회
        public List<CustomerResponseDto> getCustomers() {

                return customerRepository.findAll()
                        .stream()
                        .map(customer -> CustomerResponseDto.builder()
                                .customerId(customer.getCustomerId())
                                .name(customer.getName())
                                .phone(PhoneNumberMasker.maskMiddle(customer.getPhone()))
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
                                .phone(PhoneNumberMasker.maskMiddle(customer.getPhone()))
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

        // 담당 상담사의 고객 상태 변경
        @Transactional
        public void updateConsultantCustomerStatus(
                Long customerId,
                Long counselorId,
                String status
        ) {
                if (!codeRepository.existsByCodeGroupAndCodeValAndUseYn(
                        "CUSTOMER_STATUS",
                        status,
                        "Y"
                )) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "유효하지 않은 고객 상태입니다."
                        );
                }

                Customer customer = customerRepository
                        .findAccessibleForConsultant(customerId, counselorId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "접근 권한이 없습니다."
                        ));

                customer.setStatus(status);
        }

        // 담당 상담사의 고객 태그 변경
        @Transactional
        public void updateConsultantCustomerTag(
                Long customerId,
                Long counselorId,
                String customerTag
        ) {
                if (!codeRepository.existsByCodeGroupAndCodeValAndUseYn(
                        "CUSTOMER_TAG",
                        customerTag,
                        "Y"
                )) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "유효하지 않은 고객 태그입니다."
                        );
                }

                Customer customer = customerRepository
                        .findAccessibleForConsultant(customerId, counselorId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "접근 권한이 없습니다."
                        ));

                customer.setCustomerTag(customerTag);
        }

        // 담당 고객의 상태 및 태그를 한 번에 변경
        @Transactional
        public void updateConsultantCustomer(
                Long customerId,
                Long counselorId,
                UpdateConsultantCustomerRequestDto request
        ) {
                String status = request.getStatus();
                String customerTag = request.getCustomerTag();

                if (status == null && customerTag == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "변경할 고객 정보가 없습니다."
                        );
                }

                if (status != null && !codeRepository.existsByCodeGroupAndCodeValAndUseYn(
                        "CUSTOMER_STATUS",
                        status,
                        "Y"
                )) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "유효하지 않은 고객 상태입니다."
                        );
                }

                if (customerTag != null && !customerTag.isBlank()
                        && !codeRepository.existsByCodeGroupAndCodeValAndUseYn(
                                "CUSTOMER_TAG",
                                customerTag,
                                "Y"
                        )) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "유효하지 않은 고객 태그입니다."
                        );
                }

                Customer customer = customerRepository
                        .findAccessibleForConsultant(customerId, counselorId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "접근 권한이 없습니다."
                        ));

                if (status != null) {
                        customer.setStatus(status);
                }
                if (customerTag != null) {
                        customer.setCustomerTag(customerTag.isBlank() ? null : customerTag);
                }
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
                request.getPreferredTime(),
                request.getContent()
        );
}
}