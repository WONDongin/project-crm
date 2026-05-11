package com.project.crm.domain.customer;
// 고객 비즈니스 로직

import com.project.crm.domain.customer.dto.CustomerResponseDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

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
}