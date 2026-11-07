package com.project.crm.api.consult;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.crm.domain.customer.CustomerService;
import com.project.crm.domain.customer.dto.ConsultApplyRequestDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/consult")
@RequiredArgsConstructor
public class ConsultController {

    private final CustomerService customerService;

    // 상담 신청
    @PostMapping("/apply")
    public ResponseEntity<Void> apply(
            @Valid @RequestBody ConsultApplyRequestDto request
    ) {

        try {
            customerService.applyConsult(request);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "동일 고객의 신청이 처리되었거나 입력값이 충돌했습니다. 확인 후 다시 시도해 주세요.");
        }

        return ResponseEntity.ok().build();
    }
}