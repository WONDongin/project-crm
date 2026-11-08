package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLogRepository;
import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
import com.project.crm.domain.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
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
public class AdminDashboardService {
    private final CustomerRepository customers;
    private final UserRepository users;
    private final CodeRepository codes;
    private final ActivityLogRepository activities;

    public record Status(String code, String name, long count) {}
    public record Dashboard(long totalCustomers, long unassignedCustomers, long activeCounselors,
                            long todayConsultations, LocalDate consultationDate, List<Status> statusCounts,
                            List<CustomerPageResponseDto.Entry> recentUnassignedCustomers) {}

    public Dashboard get(String email) {
        var actor = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ROLE_ADMIN".equals(actor.getRoles()) || !"ACTIVE".equals(actor.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var statuses = new LinkedHashMap<String, Status>();
        codes.findByCodeGroupAndUseYnOrderByOrderNoAsc("CUSTOMER_STATUS", "Y")
                .forEach(code -> statuses.put(code.getCodeVal(), new Status(code.getCodeVal(), code.getCodeNm(), 0)));
        long total = 0;
        for (var row : customers.countAllStatuses()) {
            var known = statuses.get(row.getStatus());
            statuses.put(row.getStatus(), new Status(row.getStatus(),
                    known == null ? row.getStatus() : known.name(), row.getCustomerCount()));
            total += row.getCustomerCount();
        }
        var date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var recent = customers.findByCounselorIsNull(PageRequest.of(0, 5,
                        Sort.by(Sort.Direction.DESC, "createdAt", "customerId")))
                .getContent().stream().map(CustomerPageResponseDto.Entry::from).toList();
        return new Dashboard(total, customers.countByCounselorIsNull(),
                users.countByRolesAndStatus("ROLE_CONSULTANT", "ACTIVE"),
                activities.countByCreatedByAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        "CONSULTANT", date.atStartOfDay(), date.plusDays(1).atStartOfDay()),
                date, List.copyOf(statuses.values()), recent);
    }
}
