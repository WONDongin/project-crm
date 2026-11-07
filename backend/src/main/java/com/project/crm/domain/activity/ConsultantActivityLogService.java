package com.project.crm.domain.activity;

import com.project.crm.domain.activity.dto.CreateActivityLogRequestDto;
import com.project.crm.domain.code.CodeRepository;
import com.project.crm.domain.customer.CustomerRepository;
import com.project.crm.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsultantActivityLogService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CodeRepository codeRepository;

    @Transactional
    public Long create(Long customerId, String email, CreateActivityLogRequestDto request) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다."));
        if (!"ROLE_CONSULTANT".equals(author.getRoles()) || !"ACTIVE".equals(author.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 상담사만 기록을 등록할 수 있습니다.");
        }
        // 배정 변경과 동일한 고객 행을 잠가 권한 확인부터 저장까지 보호한다.
        var customer = customerRepository.findAccessibleForConsultant(customerId, author.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 고객의 상담 기록만 등록할 수 있습니다."));
        validateCodes(request);
        var now = LocalDateTime.now();
        var log = activityLogRepository.save(ActivityLog.builder()
                .customer(customer).counselor(author).createdBy("CONSULTANT")
                .consultType(request.consultType()).activityResult(request.activityResult())
                .content(request.content().strip())
                .nextAction(request.nextAction() == null ? null : request.nextAction().strip())
                .createdAt(now).build());
        if (customer.getLastActivityAt() == null || customer.getLastActivityAt().isBefore(now)) {
            customer.setLastActivityAt(now);
        }
        return log.getActivityId();
    }
    @Transactional
    public void update(Long customerId, Long activityId, String email, CreateActivityLogRequestDto request) {
        var author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다."));
        if (!"ROLE_CONSULTANT".equals(author.getRoles()) || !"ACTIVE".equals(author.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 상담사만 기록을 수정할 수 있습니다.");
        }
        customerRepository.findAccessibleForConsultant(customerId, author.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 고객의 상담 기록만 수정할 수 있습니다."));
        var log = activityLogRepository.findByActivityIdAndCustomer_CustomerId(activityId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담 기록을 찾을 수 없습니다."));
        if (!"CONSULTANT".equals(log.getCreatedBy()) || log.getCounselor() == null
                || !author.getUserId().equals(log.getCounselor().getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 상담 기록만 수정할 수 있습니다.");
        }
        validateCodes(request);
        log.setConsultType(request.consultType());
        log.setActivityResult(request.activityResult());
        log.setContent(request.content().strip());
        log.setNextAction(request.nextAction() == null ? null : request.nextAction().strip());
        log.setUpdatedAt(LocalDateTime.now());
        // 내용 수정은 새로운 상담이 아니므로 작성자, 생성일과 최근 상담일을 유지한다.
    }

    private void validateCodes(CreateActivityLogRequestDto request) {
        if ("RECEIVED".equals(request.activityResult())
                || !codeRepository.existsByCodeGroupAndCodeValAndUseYn("CONSULT_TYPE", request.consultType(), "Y")
                || !codeRepository.existsByCodeGroupAndCodeValAndUseYn("ACTIVITY_RESULT", request.activityResult(), "Y")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용 가능한 상담 유형과 결과를 선택해 주세요.");
        }
    }

}
