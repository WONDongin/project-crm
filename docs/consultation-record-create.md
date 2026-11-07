# 상담 기록 등록

- 기준: dev 95dfa439056e53534536e734fd184dcf4237fdeb
- 진입: 상담사 고객 목록 → 상담 기록 → 고객 상세 하단 상담 추가
- API: POST /consultant/customers/{customerId}/activities
- 요청: consultType, activityResult, content, nextAction(선택)
- 응답: HTTP 201, ApiResponse<Long>의 data에 생성한 activityId
- 작성자: 인증 이메일로 조회한 활성 ROLE_CONSULTANT 사용자. 요청의 작성자 정보는 사용하지 않음.
- 권한: 현재 담당 고객을 쓰기 잠금으로 조회한 뒤 등록. 미배정·다른 상담사·존재하지 않는 고객은 403.
- 공통 코드: CONSULT_TYPE, ACTIVITY_RESULT에서 useYn=Y 검증. RECEIVED는 신청 접수용이므로 등록 불가.
- 검증: 유형·결과·내용 필수, 내용 최대 10,000자, 다음 액션 최대 255자.
- 기록 저장과 고객 lastActivityAt 갱신은 동일 트랜잭션. 고객 상태와 신청 접수 원본은 변경하지 않음.
- ActivityLog.createdAt은 애플리케이션에서 저장하며, SYSTEM 접수도 PrePersist로 시각을 채움. 별도 컬럼 추가 없음.
- 저장 후 상담 이력 첫 페이지와 고객 기본 정보를 재조회. 오류 시 입력 보존, 저장 중 연속 클릭 차단.

## 검증

- TypeScript 및 변경 파일 ESLint 검사 통과.
- 외부 Google Fonts 호출을 제외한 검증용 production 빌드 통과. 실제 폰트 소스는 변경하지 않음.
- 백엔드 등록 통합 테스트 및 MVC 검증 추가. 실행 환경에서 Gradle 배포본 다운로드가 차단되어 실행하지 못함.
- 실제 MySQL 연동 저장은 로컬 환경에서 gradlew test 및 앱 실행 후 확인 필요.
