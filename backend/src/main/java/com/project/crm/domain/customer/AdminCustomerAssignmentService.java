package com.project.crm.domain.customer;

import com.project.crm.domain.activity.ActivityLog;
import com.project.crm.domain.activity.ActivityLogRepository;
import com.project.crm.domain.user.User;
import com.project.crm.domain.user.UserRepository;
import com.project.crm.domain.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 관리자페이지_담당자 배정 서비스
public class AdminCustomerAssignmentService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    // 배정 가능한 활성 상담사 목록 조회
    public List<UserResponseDto> getCounselors() {
        var counts = customerRepository.countAssignments().stream().collect(java.util.stream.Collectors.toMap(
                CustomerRepository.AssignmentCount::getUserId, CustomerRepository.AssignmentCount::getCustomerCount));
        return userRepository
                .findByRolesAndStatusOrderByNameAscUserIdAsc(
                        "ROLE_CONSULTANT",
                        "ACTIVE"
                )
                .stream()
                .map(user -> UserResponseDto.from(user, counts.getOrDefault(user.getUserId(), 0L)))
                .toList();
    }

    // 고객 담당 상담사 배정 및 변경
    @Transactional
    public void assignCounselor(Long customerId, Long counselorId, String actorEmail) {
        var lockedUsers = userRepository.findAllForAdministration();
        requireAdmin(lockedUsers, actorEmail);
        User counselor = lockedUsers.stream().filter(u -> u.getUserId().equals(counselorId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담사를 찾을 수 없습니다."));
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "고객을 찾을 수 없습니다."));

        // 요청값을 직접 변경해도 관리자 계정은 배정할 수 없음
        if (!"ROLE_CONSULTANT".equals(counselor.getRoles())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "상담사 계정만 담당자로 배정할 수 있습니다."
            );
        }

        // 목록 조회 이후 비활성화된 계정도 저장 시 다시 검증
        if (!"ACTIVE".equals(counselor.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "활성 상태인 상담사만 배정할 수 있습니다."
            );
        }

        customer.setCounselor(counselor);

        // 담당 상담사가 없는 신청 접수 기록에 배정한 상담사 연결
        List<ActivityLog> unassignedLogs = activityLogRepository
                .findByCustomer_CustomerIdAndCreatedByAndActivityResultAndCounselorIsNull(
                        customerId,
                        "SYSTEM",
                        "RECEIVED"
                );

        for (ActivityLog activityLog : unassignedLogs) {
            activityLog.setCounselor(counselor);
        }

        // 기존 고객의 상태는 유지하고 신규 고객만 진행 중으로 변경
        if ("NEW".equals(customer.getStatus())) {
            customer.setStatus("IN_PROGRESS");
        }
    }

    @Transactional
    public void unassignCounselor(Long customerId, String actorEmail) {
        requireAdmin(userRepository.findAllForAdministration(), actorEmail);
        var customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "고객을 찾을 수 없습니다."));
        customer.setCounselor(null);
        // 상태·최근 상담일·과거 상담 기록은 유지한다.
    }
    private void requireAdmin(List<User> users, String email) {
        if (users.stream().noneMatch(u -> u.getEmail().equals(email) && "ROLE_ADMIN".equals(u.getRoles()) && "ACTIVE".equals(u.getStatus())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 관리자만 담당자를 변경할 수 있습니다.");
    }
}
