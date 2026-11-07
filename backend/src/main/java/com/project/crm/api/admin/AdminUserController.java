package com.project.crm.api.admin;

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.user.AdminUserService;
import com.project.crm.domain.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/admin/users") @RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService users;
    @GetMapping public ApiResponse<List<UserResponseDto>> list() { return ApiResponse.success(users.list()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponseDto> create(@Valid @RequestBody AdminUserRequestDto.Create request,Authentication auth) {
        return ApiResponse.success(users.create(auth.getName(),request),"계정이 생성되었습니다.");
    }
    @PatchMapping("/{id}")
    public ApiResponse<AdminUserRequestDto.Change> update(@PathVariable Long id,@Valid @RequestBody AdminUserRequestDto.Update request,Authentication auth) {
        return ApiResponse.success(users.update(id,auth.getName(),request),"계정 정보가 수정되었습니다.");
    }
    @PostMapping("/{id}/password")
    public ApiResponse<AdminUserRequestDto.Change> password(@PathVariable Long id,@Valid @RequestBody AdminUserRequestDto.Password request,Authentication auth) {
        return ApiResponse.success(users.resetPassword(id,auth.getName(),request.password()),"비밀번호가 초기화되었습니다.");
    }
}
