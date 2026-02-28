# Spring Security + JWT Stateless Template (Refresh Cookie + Access Header)

Fork/Use as Template 해서 바로 시작하는 **인증/인가 템플릿**입니다.

- **Stateless**
- **Access Token**: `Authorization: Bearer <token>` (응답 헤더에도 내려줌)
- **Refresh Token**: `HttpOnly Cookie` (`refresh_token`)
- **Re-issue(재발급)**: `/api/auth/refresh`
- **Logout(로그아웃)**: `/api/auth/logout`
- **테스트 포함**: MockMvc 기반의 인증 플로우 테스트

> 목표: “프로젝트 만들 때마다 인증/인가를 매번 다시 구현하는 번거로움”을 최소화

---

## 0) 이 템플릿이 제공하는 엔드포인트

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET  /api/me` (로그인 필요, 샘플 보호 API)
- `GET  /api/admin/ping` (ADMIN 권한 필요)

---

## 1) 빠른 시작 (로컬에서 직접 따라하기)

### 1-1. 레포를 템플릿으로 사용하기
1) GitHub에서 **Use this template** 또는 Fork  
2) 프로젝트 이름/설명 변경  
3) 패키지명 변경(권장): `com.example.jwttemplate` → `your.package`

### 1-2. 설정값 넣기
`src/main/resources/application.yml` 확인:

- `jwt.secret` : 최소 32바이트 이상의 랜덤 문자열 권장
- `security.cookie.secure` : 로컬 HTTP면 `false`, 운영 HTTPS면 `true`

### 1-3. 실행
```bash
./gradlew bootRun
```

> Windows라면 `gradlew.bat bootRun` (Gradle Wrapper 포함시)

---

## 2) 테스트 유저 (시드 데이터)

앱 실행 시 기본 유저가 자동 생성됩니다.

- USER
  - email: `user@example.com`
  - password: `password1234`
- ADMIN
  - email: `admin@example.com`
  - password: `password1234`

---

## 3) 사용자 입장에서 “전체 인증 플로우” 따라하기 (curl)

### 3-1. 로그인
```bash
curl -i -X POST "http://localhost:8080/api/auth/login"   -H "Content-Type: application/json"   -d '{"email":"user@example.com","password":"password1234"}'
```

✅ 결과:
- 응답 헤더에 `Authorization: Bearer ...` (access token)
- 응답 헤더에 `Set-Cookie: refresh_token=...; HttpOnly; ...`

### 3-2. 보호 API 호출 (access token 필요)
로그인 응답의 Authorization 값을 복사해서 사용:
```bash
curl -i "http://localhost:8080/api/me"   -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 3-3. refresh로 access/refresh 재발급 (쿠키 필요)
로그인 때 받은 쿠키를 그대로 넣어 호출:
```bash
curl -i -X POST "http://localhost:8080/api/auth/refresh"   --cookie "refresh_token=<REFRESH_TOKEN>"
```

✅ 결과:
- 새 `Authorization` 헤더(access)
- 새 `Set-Cookie`(refresh 로테이션)

### 3-4. 로그아웃 (refresh 폐기 + 쿠키 삭제)
```bash
curl -i -X POST "http://localhost:8080/api/auth/logout"   --cookie "refresh_token=<REFRESH_TOKEN>"
```

✅ 결과:
- `Set-Cookie: refresh_token=; Max-Age=0; ...` 로 쿠키가 제거됩니다.
- DB에 저장된 refresh 토큰도 폐기됩니다.

---

## 4) 템플릿을 프로젝트에 맞게 바꾸는 포인트

- 권한 정책: `SecurityConfig`의 `authorizeHttpRequests` 조정
- 토큰 만료시간: `application.yml`의 TTL 변경
- 쿠키 옵션: `security.cookie.*` 변경 (SameSite, Secure, Path)
- 로그아웃에서 access token 즉시 무효화가 필요하면:
  - Redis 블랙리스트(옵션) 추가 권장

---

## 5) Gradle Wrapper 포함 권장 (최초 1회만)
템플릿 레포를 “진짜 템플릿”로 배포하려면 wrapper를 꼭 커밋하세요.

```bash
gradle wrapper --gradle-version 8.10.2
```

생성된 `gradlew`, `gradlew.bat`, `gradle/wrapper/*` 를 커밋하면
fork한 사람은 설치 없이 바로 `./gradlew` 실행이 가능합니다.

---

## License
MIT

![Last commit (main)](https://img.shields.io/github/last-commit/99hyeon/spring-security-jwt-test/main?display_timestamp=committer)
