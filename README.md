# Spring Security + JWT Stateless Template
> **Refresh Token = HttpOnly Cookie** / **Access Token = Authorization Header**  
> Fork/Use as Template 해서 바로 시작하는 **인증/인가 템플릿**

![Last commit (main)](https://img.shields.io/github/last-commit/99hyeon/spring-security-jwt-test/main?display_timestamp=committer)

---

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack & Requirements](#tech-stack--requirements)
- [Endpoints](#endpoints)
- [Quick Start](#quick-start)
  - [1) Use this template / Fork](#1-use-this-template--fork)
  - [2) Configure](#2-configure)
  - [3) Run](#3-run)
- [Seed Users (Test Data)](#seed-users-test-data)
- [Auth Flow (curl)](#auth-flow-curl)
  - [1) Login](#1-login)
  - [2) Call Protected API](#2-call-protected-api)
  - [3) Refresh (Re-issue)](#3-refresh-re-issue)
  - [4) Logout](#4-logout)
- [Customization Points](#customization-points)
- [Gradle Wrapper](#gradle-wrapper)
- [Testing](#testing)
- [License](#license)

---

## Overview
Spring Security + JWT 기반의 **Stateless 인증/인가 템플릿**입니다.

- **Access Token**: `Authorization: Bearer <token>`
  - 요청 시 헤더로 전송
  - 로그인/재발급 응답에서도 헤더로 내려줌
- **Refresh Token**: `HttpOnly Cookie` (`refresh_token`)
- **Re-issue(재발급)**: `POST /api/auth/refresh`
- **Logout(로그아웃)**: `POST /api/auth/logout`
- **MockMvc 테스트 포함**: 인증 플로우가 깨지지 않도록 기본 시나리오 테스트 제공

---

## Key Features
- ✅ **Stateless**
- ✅ Access/Refresh 분리 운영
- ✅ Refresh **Cookie 기반**
- ✅ Refresh **로테이션(재발급 시 갱신)** 흐름
- ✅ 로그아웃 시
  - 서버 저장 Refresh 토큰 폐기(soft delete)
  - 클라이언트 쿠키 `Max-Age=0`로 삭제 지시
- ✅ 샘플 보호 API 제공
  - 로그인 필요: `/api/me`
  - ADMIN 권한 필요: `/api/admin/ping`

---

## Tech Stack & Requirements

- **Java**: 21
- **Spring Boot**: 3.3.5
- **Gradle Wrapper**: 8.10.2 (레포에 포함)
- **DB (기본)**: H2 in-memory (`jdbc:h2:mem:...`, MySQL mode)
- **JWT**: JJWT 0.12.6

---

## Endpoints
| Method | Path | Auth | Description |
|------:|------|------|------------|
| POST | `/api/auth/login` | Public | 로그인 (Access 헤더 + Refresh 쿠키 발급) |
| POST | `/api/auth/refresh` | Cookie | Refresh로 Access/Refresh 재발급 |
| POST | `/api/auth/logout` | Cookie | Refresh 폐기 + 쿠키 삭제 |
| GET  | `/api/me` | Bearer | 로그인 필요 샘플 API |
| GET  | `/api/admin/ping` | Bearer + Role | ADMIN 권한 필요 |

---

## Quick Start

### 1) Use this template / Fork
1. GitHub에서 **Use this template** 또는 Fork
2. 프로젝트 이름/설명 변경
3. (권장) 패키지명 변경  
   - `com.example.jwttemplate` → `your.package`

#### 1-1) Local clone (Git Bash)
“내 컴퓨터에서 어디에 둘지” 폴더 이동

```bash
cd ~/Desktop/opensource
git clone <YOUR_REPO_URL>
cd <REPO_DIR>
```

예시(형식만 참고):

```bash
git clone https://github.com/yourname/your-repo.git
```

### 2) Configure
`src/main/resources/application.yml` 확인/수정:

- `jwt.secret`
  - **최소 32바이트 이상** 랜덤 문자열 권장
- `security.cookie.secure`
  - 로컬 HTTP: `false`
  - 운영 HTTPS: `true`

### 3) Run
```bash
./gradlew bootRun
```

Windows:
```bash
gradlew.bat bootRun
```

---

## Seed Users (Test Data)
앱 실행 시 기본 유저가 자동 생성됩니다.

- USER
  - email: `user@example.com`
  - password: `password1234`
- ADMIN
  - email: `admin@example.com`
  - password: `password1234`

---

## Auth Flow (curl)

> 쿠키를 수동으로 복붙하지 않으려면 `cookie.jar`로 저장/재사용하는 방식이 편합니다.

### 1) Login
```bash
curl -i -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -c cookie.jar \
  -d '{"email":"user@example.com","password":"password1234"}'
```

✅ 기대 결과
- 응답 헤더: `Authorization: Bearer ...` (access token)
- 응답 헤더: `Set-Cookie: refresh_token=...; HttpOnly; ...`

---

### 2) Call Protected API
로그인 응답의 `Authorization: Bearer ...` 값을 복사해서 호출:

```bash
curl -i "http://localhost:8080/api/me" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

### 3) Refresh (Re-issue)
로그인 때 저장한 쿠키로 재발급 호출:

```bash
curl -i -X POST "http://localhost:8080/api/auth/refresh" \
  -b cookie.jar \
  -c cookie.jar
```

✅ 기대 결과
- 새 `Authorization` 헤더(access)
- 새 `Set-Cookie`(refresh 로테이션)

---

### 4) Logout
```bash
curl -i -X POST "http://localhost:8080/api/auth/logout" \
  -b cookie.jar
```

✅ 기대 결과
- `Set-Cookie: refresh_token=; Max-Age=0; ...` 로 쿠키 삭제 지시
- 서버 저장 refresh 토큰 폐기

---

## Customization Points
- **권한 정책**
  - `SecurityConfig`의 `authorizeHttpRequests` 조정
- **토큰 만료시간(TTL)**
  - `application.yml`의 TTL 변경
- **쿠키 옵션**
  - `security.cookie.*` 변경 (SameSite, Secure, Path 등)
- **로그아웃에서 access token 즉시 무효화가 필요하면(옵션)**
  - Redis 블랙리스트(옵션) 추가 권장

---

<!--
## Gradle Wrapper
템플릿 레포를 “진짜 템플릿”로 배포하려면 wrapper를 꼭 커밋하세요.

```bash
gradle wrapper --gradle-version 8.10.2
```

생성된 `gradlew`, `gradlew.bat`, `gradle/wrapper/*` 를 커밋하면
fork한 사람은 설치 없이 바로 `./gradlew` 실행이 가능합니다.

---
-->

## Testing
MockMvc 기반 인증 플로우 테스트 포함.

```bash
./gradlew test
```

Windows:
```bash
gradlew.bat test
```

---

## License
MIT
