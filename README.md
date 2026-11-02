## ⭐ Education Consulting CRM (교육 상담 고객 관리 시스템)

<br>

### 소개 (Overview)

> 교육 상담 신청부터 고객 배정, 상담 진행 및 수강 전환까지의 과정을 효율적으로 관리하기 위한  
> **운영형 CRM(Customer Relationship Management) 웹 애플리케이션**입니다.
>
> - 비회원 상담 신청 → 담당 상담사 배정 → 상담 진행까지 전 과정 관리
> - 고객(Customer)과 상담 기록(Activity Log)을 분리한 이력 중심 설계
> - 관리자 / 상담사 **Role 기반 기능 및 접근 권한 분리**
> - 공통 코드 기반의 고객 상태, 상담 유형 및 처리 결과 관리
> - 실무 CRM의 데이터 흐름과 운영 방식을 고려한 백엔드 중심 프로젝트

<br>

## ⚙️ 주요 기능 (Features)

- 고객 (External User)
  - 로그인 없이 상담 신청
  - 이름, 연락처, 생년월일, 관심 과정 및 희망 상담 시간 입력
  - 전화번호 + 생년월일 기준 기존 고객 여부 확인
  - 신규 고객 자동 생성 및 `NEW` 상태 등록
  - 상담 신청 완료 페이지 제공
  - 상담 신청 시 `ActivityLog` 자동 생성
  - 상담 접수 상태(`RECEIVED`) 자동 저장

- 상담사 (CONSULTANT)
  - 로그인한 상담사 기준 담당 고객 목록 조회
  - 담당 고객의 현재 상태 및 기본 정보 확인
  - 고객 상태 변경  
    (`NEW` / `IN_PROGRESS` / `ACTIVE` / `DORMANT` / `WITHDRAWN`)
  - 상담 기록 등록 및 조회
  - 상담 유형, 상담 결과 및 상담 내용 관리
  - 본인 담당 고객만 접근할 수 있도록 서버 권한 검증

- 관리자 (ADMIN)
  - 상담사 / 관리자 계정 생성 및 관리
  - 사용자 권한 및 계정 상태 제어
  - 전체 고객 목록 조회
  - 신규 및 미지정 고객 확인
  - 고객 담당 상담사 배정 및 변경
  - 담당자 배정 완료 시 고객 상태를 `IN_PROGRESS`로 변경
  - 고객별 희망 상담 시간 및 상담 신청 정보 확인
  - 전체 운영 현황 대시보드 조회

- 인증 & 권한
  - 이메일(ID) + 비밀번호 로그인
  - JWT 기반 인증 및 `HttpOnly Cookie` 저장
  - Role 기반 접근 제어 (`ADMIN` / `CONSULTANT`)
  - 관리자·상담사 API 접근 권한 분리
  - 비활성 계정 로그인 차단
  - 프론트엔드 + 백엔드 이중 권한 검증

<br>

### ⚙️ 기술 스택 (Tech Stack)

| 구분 | 기술 |
| --- | --- |
| Frontend | Next.js (App Router), React, TypeScript, Axios, CSS Module |
| Backend | Java 17, Spring Boot 3, Spring Data JPA, Gradle |
| Auth | Spring Security, JWT, HttpOnly Cookie |
| Database | MySQL 8 |
| Architecture | Layered Architecture, REST API |
| State / Code | 공통 코드 기반 상태 관리, CodeInitializer |
| Collaboration | GitHub, Notion |

<br>

### 📂 프로젝트 구조 (Project Structure)

```bash
/backend
 ├── domain
 │   ├── user          # 관리자 / 상담사 계정 및 권한
 │   ├── customer      # 고객 정보 및 고객 상태
 │   ├── activity      # 상담 신청 및 상담 기록
 │   └── code          # 공통 코드 관리
 ├── api
 │   ├── auth          # 로그인 / 인증 API
 │   ├── consult       # 비회원 상담 신청 API
 │   ├── admin         # 관리자 전용 API
 │   └── consultant    # 상담사 전용 API
 └── config
     ├── security      # Spring Security / JWT 설정
     └── jpa           # JPA 설정

/frontend
 ├── app
 │   ├── login         # 로그인 페이지
 │   ├── consult       # 상담 신청 / 완료 페이지
 │   ├── admin         # 관리자 페이지
 │   └── consultant    # 상담사 페이지
 ├── components        # 공통 UI 컴포넌트
 ├── store             # 인증 및 공통 코드 상태 관리
 ├── lib/api           # Axios 공통 API 레이어
 └── styles            # 공통 스타일 / CSS Module
```

<br>

### 📐 전체 아키텍처 (Architecture)

```text
[Client - Next.js]
        ↓ REST API / Axios
[Spring Boot Application]
 ├── Auth API
 ├── Public Consult API
 ├── Admin API
 ├── Consultant API
 ├── Domain Service
 └── Spring Security + JWT
        ↓
[Spring Data JPA]
        ↓
[MySQL 8]
```

<br>

### 🧠 핵심 도메인 설계 (Core Logic)

#### 1. 고객 & 상담 기록 구조

- `CUSTOMER`
  - 고객의 기본 정보, 현재 상태 및 담당 상담사 관리
- `ACTIVITY_LOG`
  - 상담 신청과 고객 관련 상담 이력을 누적 관리
- 고객의 현재 상태와 과거 상담 이력을 분리하여 데이터 변경 이력 보존

```text
비회원 상담 신청
  ↓
전화번호 + 생년월일로 기존 고객 확인
  ↓
신규 고객일 경우 CUSTOMER 생성 (NEW)
  ↓
ACTIVITY_LOG 자동 생성 (RECEIVED)
  ↓
관리자가 담당 상담사 배정
  ↓
고객 상태 변경 (IN_PROGRESS)
  ↓
상담사가 상담 기록 누적
```

<br>

#### 2. 상담사 권한 흐름

- 로그인한 상담사 기준 담당 고객 조회
- 다른 상담사가 담당하는 고객은 접근 및 수정 불가
- 프론트엔드 경로 제어와 백엔드 API 권한 검증을 함께 적용
- 고객 상태 변경 및 상담 기록 등록 시 서버에서 담당자 권한 재검증

```text
상담사 로그인
  ↓
JWT 인증 및 CONSULTANT 권한 확인
  ↓
본인 담당 고객 조회
  ↓
고객 상태 변경
  ↓
상담 기록 등록 및 이력 누적
```

<br>

#### 3. 관리자 운영 흐름

- 신규 상담 고객을 관리자가 확인한 후 담당 상담사를 직접 배정
- 담당 상담사 변경 시 고객과 상담사의 연관 관계 갱신
- 담당자 배정이 완료되면 고객 상태를 상담 진행 중으로 변경
- 사용자 계정, 고객 상태 및 전체 상담 현황을 통합 관리

```text
관리자 로그인
  ↓
전체 고객 / 사용자 조회
  ↓
NEW 상태의 미지정 고객 확인
  ↓
담당 상담사 배정
  ↓
고객 상태 IN_PROGRESS 변경
  ↓
상담 진행 현황 관리
```

<br>

### 🚀 개발 및 협업 방식

- GitHub 기반 소스 코드 관리
- 기능별 브랜치 생성 및 PR 단위 병합
- `main`, `dev` 브랜치 보호 및 직접 Push 제한
- PR + 코드 리뷰 기반 협업
  - 브랜치 전략  
    `feature/*` → `dev` → `main`
- 프론트엔드와 백엔드를 기능 단위로 함께 구현
- API 요청 및 응답 구조를 DTO로 분리
- 공통 코드를 활용하여 상태값의 확장성과 일관성 유지

<br>

### 📄 배운 점 (What I Learned)

- 상담 신청부터 담당자 배정까지 이어지는 실무형 CRM 데이터 흐름 설계
- 고객의 현재 상태와 상담 이력을 분리하는 도메인 구조 이해
- Spring Security와 JWT를 활용한 인증·인가 구현
- `HttpOnly Cookie`를 활용한 토큰 저장 및 인증 처리
- 관리자와 상담사의 Role 기반 접근 제어(RBAC) 구현
- 로그인 사용자 기준 데이터 조회 및 담당자 권한 검증
- 전화번호 + 생년월일을 활용한 기존 고객 중복 확인
- 상담 신청 시 고객 및 상담 기록을 함께 저장하는 비즈니스 로직 구현
- 공통 코드 기반의 상태값 및 UI 데이터 관리
- Next.js와 Spring Boot 간 REST API 연동 경험
- Git 브랜치 전략과 PR 기반 기능 개발 경험
