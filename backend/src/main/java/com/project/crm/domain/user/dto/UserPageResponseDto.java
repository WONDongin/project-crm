package com.project.crm.domain.user.dto;

import java.util.List;

public record UserPageResponseDto(List<UserResponseDto> content, int number, int size,
                                  long totalElements, int totalPages) {}
