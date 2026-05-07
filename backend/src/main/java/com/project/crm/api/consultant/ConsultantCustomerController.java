package com.project.crm.api.consultant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 상담사 고객 목록/상세/상태 변경/상담 기록 등 라우팅
@RestController
@RequestMapping("/consultant/customers")
@RequiredArgsConstructor
public class ConsultantCustomerController {
}
