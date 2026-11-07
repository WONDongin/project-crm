package com.project.crm.domain.customer;

import com.project.crm.domain.customer.dto.CustomerPageResponseDto;
import com.project.crm.domain.user.User;
import com.project.crm.domain.user.UserRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerBrowseService {
    private final CustomerRepository customers;
    private final UserRepository users;

    public CustomerPageResponseDto search(String email, boolean admin, String keyword, String status,
            String interestCourse, Long counselorId, boolean unassigned, int page, int size) {
        User actor = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String role = admin ? "ROLE_ADMIN" : "ROLE_CONSULTANT";
        if (!role.equals(actor.getRoles()) || !"ACTIVE".equals(actor.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw bad("페이지는 0 이상, 크기는 1~100이어야 합니다.");
        }
        String term = clean(keyword, 100);
        String state = clean(status, 30);
        String course = clean(interestCourse, 30);
        if (counselorId != null && counselorId <= 0) throw bad("담당자 번호를 확인해 주세요.");
        if (counselorId != null && unassigned) throw bad("담당자와 미배정 조건을 동시에 지정할 수 없습니다.");
        if (!admin && (unassigned || (counselorId != null && !counselorId.equals(actor.getUserId())))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 담당 고객만 조회할 수 있습니다.");
        }
        // 상담사 범위는 요청값 대신 로그인 사용자로 결정한다.
        Long owner = admin ? counselorId : actor.getUserId();
        var result = customers.findAll(specification(term, state, course, owner, unassigned),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "customerId")));
        return CustomerPageResponseDto.from(result);
    }

    private Specification<Customer> specification(String keyword, String status, String course,
            Long owner, boolean unassigned) {
        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();
            if (owner != null) conditions.add(cb.equal(root.get("counselor").get("userId"), owner));
            if (unassigned) conditions.add(cb.isNull(root.get("counselor")));
            if (status != null) conditions.add(cb.equal(root.get("status"), status));
            if (course != null) conditions.add(cb.equal(root.get("interestCourse"), course));
            if (keyword != null) {
                Predicate name = cb.like(cb.lower(root.get("name")),
                        "%" + escape(keyword.toLowerCase(Locale.ROOT)) + "%", '\\');
                if (keyword.matches("[0-9() .+\\-]+") && keyword.matches(".*[0-9].*")) {
                    // 기존 하이픈 형식의 데이터도 숫자 검색어로 찾는다. 저장값은 변경하지 않는다.
                    Expression<String> phone = root.get("phone");
                    for (String separator : List.of("-", " ", "(", ")", ".", "+")) {
                        phone = cb.function("replace", String.class, phone, cb.literal(separator), cb.literal(""));
                    }
                    String digits = keyword.replaceAll("[^0-9]", "");
                    conditions.add(cb.or(name, cb.like(phone, "%" + digits + "%")));
                } else {
                    conditions.add(name);
                }
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String clean(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > max) throw bad("검색 조건의 길이를 확인해 주세요.");
        return value.strip();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
