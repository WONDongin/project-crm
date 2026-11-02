package com.project.crm.domain.activity.dto;

import com.project.crm.domain.activity.ActivityLog;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityLogPageResponseDto(
        List<Entry> content, int number, int size, long totalElements, int totalPages
) {
    public static ActivityLogPageResponseDto from(Page<ActivityLog> page) {
        return new ActivityLogPageResponseDto(page.getContent().stream().map(Entry::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public record Entry(
            Long activityId, String createdBy, String counselorName, String consultType,
            String activityResult, String preferredTime, String content, String nextAction,
            LocalDateTime createdAt
    ) {
        static Entry from(ActivityLog log) {
            return new Entry(log.getActivityId(), log.getCreatedBy(),
                    log.getCounselor() == null ? null : log.getCounselor().getName(),
                    log.getConsultType(), log.getActivityResult(), log.getPreferredTime(),
                    log.getContent(), log.getNextAction(), log.getCreatedAt());
        }
    }
}
