package com.project.crm.domain.customer;

import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.dto.UpdateCustomerProfileRequestDto;
import com.project.crm.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {
    private final CustomerRepository customers;
    private final UserRepository users;
    private final CodeRepository codes;

    public static String normalizePhone(String value) {
        if (value == null || !value.matches("[0-9() .+-]{9,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연락처 형식을 확인해 주세요.");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!digits.matches("[0-9]{9,11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연락처는 숫자 9~11자리로 입력해 주세요.");
        }
        return digits;
    }

    @Transactional
    public void update(Long customerId, String email, UpdateCustomerProfileRequestDto request) {
        var user = users.findByEmailForWork(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ROLE_CONSULTANT".equals(user.getRoles()) || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var customer = customers.findAccessibleForConsultant(customerId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        String phone = normalizePhone(request.phone());
        if (!codes.existsByCodeGroupAndCodeValAndUseYn("INTEREST_COURSE", request.interestCourse(), "Y")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용 가능한 관심 과정을 선택해 주세요.");
        }
        if (!customers.findIdentityMatches(phone, customer.getBirthDate(), customerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 연락처와 생년월일로 등록된 고객이 있습니다.");
        }
        customer.setName(request.name().strip());
        customer.setPhone(phone);
        customer.setInterestCourse(request.interestCourse());
        // 신청 원본(ActivityLog), 생년월일, 담당자, 상태와 최근 상담일은 변경하지 않는다.
    }
}
