## ⭐ Education Consulting CRM (교육 상담 고객 관리 시스템)

<BR>

### 소개 (Overview)

> 교육 상담 및 수강생 관리를 효율적으로 수행하기 위한 <bR> **운영형 CRM(Customer Relationship Management) 웹 애플리케이션**입니다.
> - 상담 신청 → 상담 진행 → 수강 전환까지 전 과정 관리
> - 고객(Customer)과 상담 기록(Activity Log)을 분리한 상태 중심 설계
> - 관리자 / 상담사 **Role 기반 기능 분리**
> - 실무 CRM 구조를 기준으로 한 백엔드 중심 프로젝트

<bR>

## ⚙️ 주요 기능 (Features)

- 고객 (External User)
  - 비회원 상담 신청
  - 상담 내용 접수

- 상담사 (CONSULTANT)
  - 담당 고객 관리
  - 고객 상태 변경  
    (`NEW` / `IN_PROGRESS` / `ACTIVE` / `DORMANT` / `WITHDRAWN`)
  - 상담 기록 등록 및 조회
  - 미지정 고객 최초 접근 시 자동 담당자 지정

- 관리자 (ADMIN)
  - 상담사 / 관리자 계정 관리
  - 사용자 권한 및 상태 제어
  - 고객 담당 상담사 변경
  - 전체 운영 현황 대시보드 조회

- 인증 & 권한
  - 이메일(ID) + 비밀번호 로그인
  - Role 기반 접근 제어 (`ADMIN` / `CONSULTANT`)
  - 비활성 계정 로그인 차단

<BR>

### ⚙️ 기술 스택 (Tech Stack)

| 구분 | 기술 |
| --- | --- |
| Frontend | Next.js (App Router), React, Axios |
| Backend | Spring Boot, JPA, QueryDSL |
| Auth | Spring Security (Session 기반) |
| Database | MySQL 8 |
| Architecture | Layered Architecture |
| Collaboration | GitHub, Notion |

<BR>

### 📂 프로젝트 구조 (Project Structure)

```bash
/backend
 ├── domain
 │   ├── user          # 사용자 (관리자 / 상담사)
 │   ├── customer      # 고객
 │   ├── activity      # 상담 기록
 │   ├── code          # 공통 코드 관리
 │   └── file          # 파일 관리
 ├── api
 │   ├── admin         # 관리자 API
 │   └── consultant    # 상담사 API
 └── config            # Security / JPA / Scheduler 설정

/frontend
 ├── app               # Next.js App Router
 ├── features          # 기능 단위 UI 로직
 ├── components        # 공통 UI 컴포넌트
 └── lib/api           # Axios 공통 API 레이어
```

<bR>

### 📐 전체 아키텍처 (Architecture)
```bash
[Client - Next.js]
↓ (REST API)
[Spring Boot Application]
├─ Admin API
├─ Consultant API
├─ Domain Service
└─ Security (RBAC)
↓
[MySQL]
```
<Br>

### 🧠 핵심 도메인 설계 (Core Logic)
### 1. 고객 & 상담 기록 구조
- `CUSTOMER` : 고객의 현재 상태 관리
- `ACTIVITY_LOG` : 고객과 관련된 모든 상담 이력 누적
```text
상담 신청
  ↓
CUSTOMER 생성 (NEW)
  ↓
ACTIVITY_LOG 자동 생성
  ↓
상담 진행
  ↓
고객 상태 변경
```

<br>

### 2. 상담사 권한 흐름
- 다른 상담사 담당 고객 접근 불가
- UI + 서버 이중 권한 검증
```text
상담사 로그인
  ↓
본인 담당 고객 조회
  ↓
미지정 고객 최초 상담
  → 담당자 자동 지정
```

<Br>

### 3. 관리자 운영 흐름
```text
관리자 로그인
  ↓
전체 고객 / 상담사 조회
  ↓
계정 및 담당자 관리
```

<Br>

### 🚀 개발 및 협업 방식

- GitHub 기반 협업
- PR + 코드 리뷰
  - 브랜치 전략
  `feature` → `dev` → `main`)
- 기능 단위 개발 및 병합

<br>

### 📄 배운 점 (What I Learned)

- 실무형 CRM 도메인 설계 경험
- 고객 / 상담 이력 분리 구조 이해
- Role 기반 접근 제어(RBAC)
- QueryDSL 조건 검색 및 페이징
- 백엔드 중심 설계 및 협업 경험
