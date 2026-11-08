# 관리자 대시보드

`GET /admin/dashboard`는 로그인한 활성 관리자만 사용할 수 있다.
기존 ApiResponse 형식의 data에 다음 값을 반환한다.

| 필드 | 기준 |
| --- | --- |
| totalCustomers | 전체 고객 수 (상태별 집계 합계) |
| unassignedCustomers | 담당 상담사가 없는 고객 수, 상태와 무관 |
| activeCounselors | ROLE_CONSULTANT이면서 ACTIVE인 계정 수 |
| todayConsultations | 한국 시간 오늘 00:00 이상, 다음 날 00:00 미만에 등록한 CONSULTANT 상담 기록 수 |
| consultationDate | 오늘 상담 집계 기준일 (Asia/Seoul) |
| statusCounts | 활성 CUSTOMER_STATUS 코드의 0명 상태 포함. 고객이 있는 비활성·미등록 코드도 포함 |
| recentUnassignedCustomers | 미배정 고객 중 등록일 내림차순, 같은 시각이면 고객 ID 내림차순 5명. 연락처 마스킹 |

오늘 상담은 고객 수가 아닌 상담 기록 수다. SYSTEM 신청 접수와 과거 기록 수정은 오늘 상담 건수에 추가하지 않는다.
created_at은 기존 모델의 LocalDateTime을 한국 현지 시각으로 취급한다. 서버 및 DB의 기록 시각 정책도 한국 시간으로 맞춰야 한다.

관리자 대시보드의 담당자 배정 버튼과 미배정 고객 목록 링크는
`/admin/customers?counselor=unassigned`로 이동하며, 고객 목록에서 미배정 필터를 초기 적용한다.
상태별 현황은 `/admin/customers?status=...`로 이동한다.

새로고침 시 기존 결과를 유지하고, 조회 실패 시 오류와 재시도 버튼을 표시한다.
초기 조회 실패를 0명으로 표시하지 않으며, 데이터가 없으면 0명과 빈 목록 안내를 표시한다.

## 검증

- AdminDashboardIntegrationTest: 전체·미배정 집계, 활성 상담사 기준, 상태 합계 및 0명 상태, 최근 5명, 연락처 마스킹, 담당자 배정 후 반영, 한국 날짜 경계와 SYSTEM 제외, 비활성 관리자 차단.
- AdminDashboardControllerTest: 미인증 401, 상담사 403, 관리자 조회 성공 및 로그인 사용자 식별.
- 화면 수동 확인: 관리자 로그인 → 요약 및 상태 집계 확인 → 미배정 필터 이동 → 담당자 배정 → 대시보드 새로고침 → 수치 변경 확인.

### 이번 작업 실행 결과

- JDK 17 / Gradle 8.14.4 전체 백엔드 테스트 통과.
- Next.js 프로덕션 빌드, TypeScript 검사 및 변경 화면 ESLint 통과.
- 브라우저 화면 검증은 실행 환경의 로컬 서버 연결 제한으로 미완료. 실제 MySQL 연결 후 화면 동작 확인 필요.
