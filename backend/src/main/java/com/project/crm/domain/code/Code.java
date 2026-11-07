package com.project.crm.domain.code;
// 코드 엔티티 (CODE_MNG)

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_mng")
@IdClass(CodeId.class)
@Getter
@Setter
public class Code {
    @Id
    @Column(name = "code_group", nullable = false, length = 50)
    private String codeGroup;

    @Id
    @Column(name = "code_val", nullable = false, length = 50)
    private String codeVal;

    @Column(name = "code_nm", nullable = false, length = 100)
    private String codeNm;

    @Column(name = "parent_code", length = 50)
    private String parentCode;

    @Column(name = "depth", nullable = false)
    private Integer depth = 0;

    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String useYn = "Y";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @PrePersist
    void initializeDefaults() {
        if (depth == null) depth = 0;
        if (useYn == null) useYn = "Y";
        if (createdAt == null) createdAt = LocalDateTime.now().withNano(0);
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now().withNano(0);
    }
}
