# 고객 기본 정보 수정 및 상담 첨부파일

## 실행 전 DB 적용

현재 설정은 ddl-auto=none이므로 `backend/db/migrations/20261107_customer_profile_attachments.sql`을 MySQL 8에 수동 적용한다.

1. 먼저 SQL 상단 중복 고객 조회만 실행한다. 결과가 있으면 연락처/생년월일이 같은 고객을 확인하고 정리한 후 진행한다. 스크립트는 고객을 자동 병합하거나 삭제하지 않는다.
2. customers에 정규화된 연락처/생년월일 generated column과 UNIQUE 인덱스를 추가한다. 기존 연락처 값은 변경하지 않는다.
3. activity_attachments 테이블을 생성한다.
4. MySQL max_allowed_packet이 32MB 이상인지 확인한다. 최대 파일은 10MB지만 쿼리 전송 오버헤드가 있으므로 여유를 둔다.
5. 로컬/프록시에 별도 multipart 제한이 있다면 파일 10MB, 요청 11MB 이상을 허용한다.

SQL은 반복 실행용이 아니다. 이미 적용한 DB에서는 다시 실행하지 않는다. 운영 DB 적용은 실행하지 않았다.

## 고객 기본 정보

- PATCH /consultant/customers/{customerId}/profile
- 요청: name(필수, 최대50), phone(필수, 서식 포함 최대20), interestCourse(활성 공통 코드).
- 현재 담당자인 활성 상담사만 수정. 저장 시 고객 행 잠금 후 배정 재검증.
- 연락처는 숫자 9~11자리로 정규화한다. 기존 서식이 있는 DB 값도 하이픈/공백/괄호/마침표/+를 제거한 값으로 비교한다.
- 연락처 + 생년월일 기준 중복 검사(자기 자신 제외), 중복은409. DB UNIQUE 인덱스로 동시 요청도 보호.
- 상담 신청도 정규화된 연락처와 생년월일로 기존 고객을 찾아 수정 후 재신청이 같은 고객에 누적되도록 보완.
- 이름·연락처·관심 과정만 변경. ActivityLog 신청 원문, 생년월일, 상태, 담당자, 최근 상담일 유지.

## 첨부파일

경로: /consultant/customers/{customerId}/activities/{activityId}/attachments

- GET: 메타데이터 목록. 파일 본문은 목록 projection에서 제외.
- POST: multipart/form-data, file 파트 1개. 생성 파일 ID 반환(201).
- GET /{fileId}: 다운로드, Content-Disposition attachment, UTF-8 파일명, nosniff, no-store.
- DELETE /{fileId}: 메타데이터/본문 삭제.

지원: PNG/JPG/JPEG/GIF/WEBP, PDF, UTF-8 TXT, DOCX/XLSX/PPTX. HWP, DOC/XLS/PPT, ZIP, 실행파일은 지원하지 않는다.

- 파일당10MB, 기록당10개. 빈 파일 거부.
- 파일명에서 경로/제어문자 제거, 최대200자.
- 요청 MIME을 신뢰하지 않고 확장자와 매직 바이트 검사. TXT는 UTF-8·제어문자, Office 문서는 내부 ZIP 필수 파일 검사.
- Office 검사 중 엔트리2000개, 전체 해제100MB 초과 거부. 파일 내부 문서를 렌더링하거나 실행하지 않음.
- 형식 검사는 악성코드 백신 검사를 의미하지 않는다.
- 현재 담당자는 과거 작성자의 첨부도 목록 조회/다운로드 가능.
- 추가/삭제는 현재 담당 고객 + 본인 작성 CONSULTANT 기록만 허용. SYSTEM 원본 변경 차단.
- 고객/기록/파일 ID 조합을 매번 검사. 고객 행 잠금으로 배정 변경과 개수 제한 검사 직렬화.
- 파일은 LONGBLOB으로 DB에 저장하여 메타데이터/본문을 한 트랜잭션으로 관리. 별도 업로드 폴더 불필요.
- 기존 activity_logs.file_upload_no는 변경하지 않고 신규1:N 테이블 사용.

## 화면

고객 상세 기본 정보 하단에 수정 폼. 각 상담 기록 하단 첨부파일 보기를 열어 목록을 조회하고 업로드/다운로드/삭제.

저장/업로드 중 연속 클릭 차단, 오류 시 입력/선택 유지, 삭제 확인·취소, 권한 거부 시 화면 차단. 이미지 인라인 미리보기는 제공하지 않고 다운로드 방식 사용.

## 검증

- TypeScript/변경 파일 ESLint 통과.
- 외부 Google Fonts 호출을 임시 제외한 production 빌드 통과. 폰트 소스는 복구했고 변경을 커밋하지 않음.
- production + 모의 API 브라우저 통과: 고객 중복409 입력 유지·수정 후 재조회, PNG/PDF 업로드, 다운로드 파일명과 본문, 삭제 확인/취소, 이전 작성자 읽기 전용,10MB 초과 클라이언트 차단, 형식400 입력 유지,403 접근 제한.
- 1440/768/390px 가로 넘침 검사 및 스크린샷 확인.
- 백엔드 통합/MVC/파일 검증 테스트 추가. Gradle 다운로드가 Network is unreachable로 실패하여 실행하지 못함.
- MySQL 마이그레이션, 실제 파일/DB 저장과 다운로드, 최대 크기·동시 요청은 추가 확인 필요.
