package com.project.crm.api.admin;
// /api/admin/users/** 엔드포인트 (사용자 관리 기능 라우팅)

import com.project.crm.common.response.ApiResponse;
import com.project.crm.domain.user.UserService;
import com.project.crm.domain.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private  final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponseDto>> getUsers() {

        return ApiResponse.success(userService.getUsers());
    }
}
