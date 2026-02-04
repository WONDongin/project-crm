# 📘 CRM 프로젝트 (교육 상담 CRM)

교육 상담 과정을 운영하는 기관을 위한 **운영형 CRM(Customer Relationship Management)** 시스템입니다.  
상담 신청 → 고객 관리 → 상담 기록 → 관리자/상담사 권한 분리를 중심으로 설계되었습니다.

---

## 🔍 프로젝트 개요

- **프로젝트 유형**: 교육 상담 CRM (운영형 CRM)
- **주요 사용자**
    - 관리자 (ADMIN)
    - 상담사 (CONSULTANT)
    - 고객 (비회원 상담 신청)
- **핵심 도메인**
    - 고객(Customer)
    - 상담 기록(Activity Log)
    - 사용자(User)
    - 공통 코드(Code Management)

---

## 🧱 기술 스택

### Backend
- Java 17
- Spring Boot 3.x
- Spring Security
- JPA / QueryDSL
- MySQL 8.x

### Frontend
- React
- Next.js (App Router)
- Axios
- CSS Module

### Infra / 협업
- GitHub
- Notion
- MySQL + HeidiSQL

---

## 📂 프로젝트 구조

```text
crm-project
├─ backend/        # Spring Boot 백엔드
└─ frontend/       # Next.js 프론트엔드
```

---

## 🔑 핵심 기능

1️⃣ 인증 / 권한

- 내부 사용자 로그인 (이메일 + 비밀번호)
- 역할 기반 접근 제어 (ADMIN / CONSULTANT)

2️⃣ 상담 신청 (고객)

- 비회원 상담 신청 가능
- 연락처 + 생년월일 기준 중복 고객 판단
- 상담 신청 시 초기 상담 기록 자동 생성

3️⃣ 상담사 기능

- 본인 담당 고객 + 미지정 고객 조회
- 고객 상태 변경
- 상담 기록 등록 / 조회

4️⃣ 관리자 기능

- 사용자(상담사) 관리
- 고객 전체 조회
- 담당 상담사 변경

---

## ⚙️ 실행 방법

1️⃣ Backend 실행
```bash
cd backend
./gradlew bootRun
```

- 접속: http://localhost:8080

---

2️⃣ Frontend 실행
```bash
cd frontend/crm-next
npm install
npm run dev
```

- 접속: http://localhost:3000

---

## 🗄️ DB 설정

- DB: MySQL 8.x
- 스키마: crm
- 로컬 DB 설정은 개인별로 분리

```bash
application.yml          # 공통 설정 (Git 관리)
application-local.yml    # 개인 로컬 설정 (Git ignore)
```

- application-local.yml 사용
- Git 추적 제외 (.gitignore)

---

## 📂 프로젝트 구조
```text
crm-project
├─ backend/        # Spring Boot 백엔드
└─ frontend/       # Next.js 프론트엔드
```

## 🌿 Git 브랜치 전략
```text
main    : 배포 기준 브랜치
dev     : 통합 개발 브랜치
feature : 기능 단위 작업 브랜치
```
### 작업 흐름
```text
feature/* → dev → main
```
- main 직접 push ❌
- 모든 병합은 PR 기반

---

## 📝 커밋 메시지 규칙
```text
feat:     기능 추가
fix:      버그 수정
refactor: 리팩토링
docs:     문서 수정
chore:    설정 / 환경
```

예시:
```text
feat: 고객 목록 조회 API 구현
chore: datasource 설정 분리
```

---

## 📄 문서

- 기능 정의서
- 화면 정의서
- API 명세서
- ERD 
- 모든 문서는 Notion에서 관리

---

## 📌 참고

- 이 프로젝트는 실무 CRM 구조를 학습 목적으로 설계되었습니다.
- 상태 중심 도메인 설계 + RBAC 권한 분리를 핵심으로 합니다.