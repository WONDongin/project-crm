package com.project.crm.api.admin;

import com.project.crm.common.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages="com.project.crm.api.admin")
public class AdminExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ApiResponse.fail(exception.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("입력값의 필수 항목·형식·길이를 확인해 주세요."));
    }
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> locked(org.springframework.dao.PessimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("다른 변경이 처리 중입니다. 목록을 다시 확인한 후 시도해 주세요."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("중복 이메일 또는 입력값을 확인해 주세요."));
    }
}
