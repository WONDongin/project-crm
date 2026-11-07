package com.project.crm.domain.customer.dto;

import com.project.crm.common.util.PhoneNumberMasker;
import com.project.crm.domain.customer.Customer;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerPageResponseDto(List<Entry> content, int number, int size, long totalElements, int totalPages) {
    public static CustomerPageResponseDto from(Page<Customer> page) {
        return new CustomerPageResponseDto(page.getContent().stream().map(Entry::from).toList(),
                page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
    }
    public record Entry(Long customerId,String name,String phone,String interestCourse,String status,String customerTag,
                        Long counselorId,String counselorName,LocalDateTime lastActivityAt,String leadSource,LocalDateTime createdAt) {
        public static Entry from(Customer customer) {
            return new Entry(customer.getCustomerId(),customer.getName(),PhoneNumberMasker.maskMiddle(customer.getPhone()),
                    customer.getInterestCourse(),customer.getStatus(),customer.getCustomerTag(),
                    customer.getCounselor()==null?null:customer.getCounselor().getUserId(),
                    customer.getCounselor()==null?null:customer.getCounselor().getName(),
                    customer.getLastActivityAt(),customer.getLeadSource(),customer.getCreatedAt());
        }
    }
}
