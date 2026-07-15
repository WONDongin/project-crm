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

        customerService.applyConsult(request);

        return ResponseEntity.ok().build();
    }
}