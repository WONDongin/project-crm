# 상담사 고객 상세·신청 정보 조회

- 목록의 고객명을 누르면 `/consultant/customers/{customerId}` 상세 화면으로 이동한다.
- API: `GET /consultant/customers/{customerId}`. 상담사 ID는 요청에서 받지 않고 로그인 사용자로 결정한다.
- 기본 정보: 이름, 전체 연락처, 생년월일, 이메일, 관심 과정, 유입 경로, 상태, 태그, 담당 상담사, 최근 상담일, 고객 등록일.
- 신청 정보: `createdBy=SYSTEM`, `activityResult=RECEIVED`인 기록 중 접수일 내림차순, 동일 접수일에서는 기록 ID 내림차순으로 한 건을 반환한다. 일반 상담 기록은 포함하지 않는다.
- 반환 형식은 기존 `ApiResponse`를 유지하고 `data.application`에 `activityId`, `preferredTime`, `content`, `receivedAt`을 담는다. 신청 기록이 없으면 `application`은 `null`이다.
- 다른 상담사의 고객, 미배정 고객, 없는 고객은 모두 403으로 처리하여 존재 여부를 별도로 공개하지 않는다. 담당자 변경 후에는 현재 담당자에게만 조회를 허용한다.
- 상세 화면은 조회 전용이다. 조회로 최근 상담일·고객 상태·신청 원본을 변경하지 않는다. 목록 연락처 마스킹도 유지한다.

## 검증

- `backend`: `./gradlew test` (Windows: `.\gradlew.bat test`). H2를 사용하므로 운영 DB 연결은 필요 없다.
- 통합 테스트 7개: 담당 고객 정보·신청 원본 조회, 타 상담사·미배정·미존재 접근 차단, 담당자 변경, 신청 없음, 최신 신청 선택·일반 기록 제외.
- HTTP 테스트 4개: 로그인 사용자 ID 적용·요청 ID 위조 무시, 비로그인·관리자 차단, 담당 권한 거절, 삭제된 로그인 사용자 거절.
- `frontend/crm-next`: `npx tsc --noEmit`, 변경 파일 ESLint, `npm run build`.
- 브라우저 확인: 목록↔상세 이동, 공통코드 이름, 신청 없음, 403에서 고객 데이터 숨김, 오류 후 재시도, 로딩, 잘못된 ID, 1440/768/390px 가로 넘침. 브라우저 확인에서는 API 응답을 모의했다.
- DB 테스트는 H2 기반이다. 실제 MySQL 및 로그인 세션을 연결한 확인은 로컬에서 별도로 진행한다.
