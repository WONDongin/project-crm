package com.project.crm.domain.code;
// 코드 엔티티 (CODE_MNG)

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CODE_MNG")
@IdClass(CodeId.class)
@Getter
@Setter
public class Code {
    @Id
    @Column(name = "code_group")
    private String codeGroup;

    @Id
    @Column(name = "code_val")
    private String codeVal;

    @Column(name = "code_nm")
    private String codeNm;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "depth")
    private Integer depth;

    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "use_yn")
    private String useYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
