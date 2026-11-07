package com.project.crm.domain.customer;

import com.project.crm.domain.code.Code;
import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
import com.project.crm.domain.user.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultantDashboardService {
    private final CustomerRepository customers;
    private final UserRepository users;
    private final CodeRepository codes;

    public record Status(String code, String name, long count) {}
    public record Dashboard(long totalCustomers, List<Status> statusCounts,
                            List<CustomerPageResponseDto.Entry> recentCustomers) {}

    public Dashboard get(String email) {
        var actor = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ROLE_CONSULTANT".equals(actor.getRoles()) || !"ACTIVE".equals(actor.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Map<String, Status> statuses = new LinkedHashMap<>();
        codes.findByCodeGroupAndUseYnOrderByOrderNoAsc("CUSTOMER_STATUS", "Y").stream()
                .sorted(Comparator.comparing(Code::getOrderNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Code::getCodeVal))
                .forEach(code -> statuses.put(code.getCodeVal(), new Status(code.getCodeVal(), code.getCodeNm(), 0)));

        // 활성 코드에 해당 고객이 없으면 0명으로 표시한다. 기존 비활성/미등록 상태도 합계에 포함한다.
        long total = 0;
        for (var row : customers.countStatusesForCounselor(actor.getUserId())) {
            var known = statuses.get(row.getStatus());
            statuses.put(row.getStatus(), new Status(row.getStatus(),
                    known == null ? row.getStatus() : known.name(), row.getCustomerCount()));
            total += row.getCustomerCount();
        }
        var recent = customers.findByCounselor_UserIdAndLastActivityAtIsNotNull(actor.getUserId(),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "lastActivityAt", "customerId")))
                .getContent().stream().map(CustomerPageResponseDto.Entry::from).toList();
        return new Dashboard(total, List.copyOf(statuses.values()), recent);
    }
}
