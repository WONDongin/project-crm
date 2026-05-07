package com.project.crm.domain.code;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 코드 조회/관리 Repository
@Repository
public interface CodeRepository extends JpaRepository<Code, CodeId> {
    List<Code> findByCodeGroupAndUseYnOrderByOrderNoAsc(
            String codeGroup,
            String useYn
    );
}
