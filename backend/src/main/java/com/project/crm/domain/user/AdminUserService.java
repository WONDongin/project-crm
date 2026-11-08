package com.project.crm.domain.user;

import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.CustomerRepository;
import com.project.crm.domain.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminUserService {
    private final UserRepository users;
    private final CustomerRepository customers;
    private final CodeRepository codes;
    private final BCryptPasswordEncoder passwords;

    public UserPageResponseDto list(String actorEmail, String keyword, String roles, String status,
                                    int page, int size) {
        var actor = users.findByEmail(actorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ROLE_ADMIN".equals(actor.getRoles()) || !"ACTIVE".equals(actor.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 관리자만 사용자를 조회할 수 있습니다.");
        }
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw badSearch("페이지는 0 이상, 크기는 1~100이어야 합니다.");
        }
        String term = searchValue(keyword, 100);
        String role = searchValue(roles, 20);
        String state = searchValue(status, 20);
        Specification<User> conditions = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!role.isEmpty()) predicates.add(cb.equal(root.get("roles"), role));
            if (!state.isEmpty()) predicates.add(cb.equal(root.get("status"), state));
            if (!term.isEmpty()) {
                // %, _와 escape 문자도 검색어 그대로 비교한다.
                String pattern = "%" + term.toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.like(cb.lower(root.get("email")), pattern, '!')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = users.findAll(conditions,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "userId")));
        var ids = result.getContent().stream().map(User::getUserId).toList();
        Map<Long, Long> counts = ids.isEmpty() ? Map.of() : customers.countAssignmentsForUsers(ids).stream()
                .collect(Collectors.toMap(CustomerRepository.AssignmentCount::getUserId,
                        CustomerRepository.AssignmentCount::getCustomerCount));
        var content = result.getContent().stream()
                .map(user -> UserResponseDto.from(user, counts.getOrDefault(user.getUserId(), 0L))).toList();
        return new UserPageResponseDto(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private String searchValue(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() > maxLength) throw badSearch("검색 조건의 길이를 확인해 주세요.");
        return value.strip();
    }

    private ResponseStatusException badSearch(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @Transactional
    public UserResponseDto create(String actorEmail, AdminUserRequestDto.Create request) {
        var locked=users.findAllForAdministration(); requireAdmin(locked,actorEmail);
        validateCodes(request.roles(),request.specialty(),request.status());
        String email=normalizeEmail(request.email());
        if(users.existsByEmailIgnoreCase(email)) throw conflict("이미 사용 중인 이메일입니다.");
        var user=User.builder().email(email).name(request.name().strip()).password(passwords.encode(request.password()))
                .roles(request.roles()).specialty(request.specialty()).status(request.status()).memo(clean(request.memo())).build();
        return UserResponseDto.from(users.saveAndFlush(user));
    }
    @Transactional
    public AdminUserRequestDto.Change update(Long id,String actorEmail,AdminUserRequestDto.Update request) {
        var locked=users.findAllForAdministration(); var actor=requireAdmin(locked,actorEmail);
        var target=find(locked,id); validateCodes(request.roles(),request.specialty(),request.status());
        boolean losesAdmin= !"ROLE_ADMIN".equals(request.roles()) || !"ACTIVE".equals(request.status());
        if(actor.getUserId().equals(id) && losesAdmin) throw conflict("본인의 관리자 권한이나 활성 상태는 해제할 수 없습니다.");
        if("ROLE_ADMIN".equals(target.getRoles()) && "ACTIVE".equals(target.getStatus()) && losesAdmin
                && locked.stream().filter(u -> "ROLE_ADMIN".equals(u.getRoles()) && "ACTIVE".equals(u.getStatus())).count()<=1)
            throw conflict("마지막 활성 관리자는 해제할 수 없습니다.");
        String email=normalizeEmail(request.email());
        if(users.existsByEmailIgnoreCaseAndUserIdNot(email,id)) throw conflict("이미 사용 중인 이메일입니다.");
        boolean revoke= !target.getEmail().equals(email) || !target.getRoles().equals(request.roles()) || !target.getStatus().equals(request.status());
        long released=0;
        if(!"ROLE_CONSULTANT".equals(request.roles()) || !"ACTIVE".equals(request.status())) {
            var assigned=customers.findAssignedForUpdate(id);
            assigned.forEach(c -> c.setCounselor(null)); released=assigned.size();
        }
        target.setEmail(email);target.setName(request.name().strip());target.setRoles(request.roles());
        target.setSpecialty(request.specialty());target.setStatus(request.status());target.setMemo(clean(request.memo()));
        if(revoke) target.revokeTokens();
        users.flush();
        long count=assignedCount(id);
        return new AdminUserRequestDto.Change(UserResponseDto.from(target,count),released,actor.getUserId().equals(id)&&revoke);
    }
    @Transactional
    public AdminUserRequestDto.Change resetPassword(Long id,String actorEmail,String password) {
        var locked=users.findAllForAdministration(); var actor=requireAdmin(locked,actorEmail); var target=find(locked,id);
        target.setPassword(passwords.encode(password)); target.revokeTokens(); users.flush();
        return new AdminUserRequestDto.Change(UserResponseDto.from(target,assignedCount(id)),0,actor.getUserId().equals(id));
    }
    private long assignedCount(Long id) {
        return customers.countAssignments().stream().filter(c -> id.equals(c.getUserId()))
                .mapToLong(CustomerRepository.AssignmentCount::getCustomerCount).sum();
    }
    private User requireAdmin(List<User> locked,String email) {
        return locked.stream().filter(u -> u.getEmail().equals(email) && "ROLE_ADMIN".equals(u.getRoles()) && "ACTIVE".equals(u.getStatus()))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"활성 관리자만 변경할 수 있습니다."));
    }
    private User find(List<User> locked,Long id) {
        return locked.stream().filter(u -> u.getUserId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"사용자를 찾을 수 없습니다."));
    }
    private void validateCodes(String role,String specialty,String status) {
        if(!List.of("ROLE_ADMIN","ROLE_CONSULTANT").contains(role)
                || !codes.existsByCodeGroupAndCodeValAndUseYn("USER_ROLE",role,"Y")
                || !codes.existsByCodeGroupAndCodeValAndUseYn("USER_SPECIALTY",specialty,"Y")
                || !codes.existsByCodeGroupAndCodeValAndUseYn("USER_STATUS",status,"Y"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"사용 가능한 역할·전공·상태를 선택해 주세요.");
    }
    private String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private String clean(String memo) { return memo==null || memo.isBlank()?null:memo.strip(); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
