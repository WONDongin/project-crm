# 상담 기록 수정

- 기준: dev ca7df17031beb45afd60c29a41149893fac9d596
- 브랜치: feature/consultation-record-edit
- API: PATCH /consultant/customers/{customerId}/activities/{activityId}
- 요청: consultType, activityResult, content, nextAction. 등록과 동일한 입력 검증 사용.
- 작성자: JWT 인증 이메일로 조회한 활성 ROLE_CONSULTANT 사용자.
- 조건: 현재 담당 고객이며 createdBy=CONSULTANT인 본인 작성 기록만 수정 가능.
- SYSTEM 기록은 담당 상담사가 연결되어 있어도 수정 불가.
- 고객 행에 쓰기 잠금 적용 후 권한 확인과 기록 변경 처리. 담당자 변경 후 이전 담당자는 접근 불가, 새 담당자도 이전 담당자 기록은 수정 불가.
- 기록 ID와 고객 ID를 함께 조회하여 다른 고객 기록 ID를 조합한 요청 차단.
- 변경 범위: 상담 유형·결과·내용·다음 액션·수정일. 작성자·생성 주체·작성일·최근 상담일 유지.
- 상담 이력 응답에 editable과 updatedAt 추가. 서버가 수정 버튼 표시 여부를 계산하며 저장 시 다시 검증.
- 등록 폼을 재사용하여 기존 값 표시, 저장 후 현재 페이지 재조회, 닫기 시 편집 취소, 오류 시 입력 유지.
- 기존 코드가 비활성화된 경우 사용 가능한 코드로 다시 선택하도록 표시.
- DB 컬럼 추가 없음.

## 검증

- TypeScript 및 변경 파일 ESLint 통과.
- 외부 Google Fonts 호출만 임시 제외한 검증용 production 빌드 통과. 폰트 소스 변경은 커밋에 포함하지 않음.
- production 빌드 + 모의 API 브라우저 검증 통과: 수정 버튼 표시, 기존 값 입력, 저장 후 재조회, 편집 취소, 오류 시 입력 유지, 403 시 접근 제한.
- 1440 / 768 / 390px 스크린샷 확인 및 가로 넘침 검사 통과.
- 백엔드 통합·MVC 테스트 추가: 본인 기록, SYSTEM 차단, 재배정, 고객/기록 ID 불일치, 비활성 계정과 코드, 입력 검증, 작성일/최근 상담일 보존, editable 응답.
- Gradle 배포본 다운로드가 Network is unreachable로 실패하여 백엔드 자동 테스트는 실행하지 못함. 실제 MySQL 연동 확인 필요.
