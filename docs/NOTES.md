## Notes

- Cookie name은 현재 Controller의 @CookieValue(name="refresh_token") 에 하드코딩되어 있습니다.
  템플릿 사용자 편의 때문에 기본값을 고정해두었고, 필요하면 아래 중 하나로 확장하세요.

  1) HandlerMethodArgumentResolver로 cookieName을 properties로 읽어서 주입
  2) OncePerRequestFilter에서 cookie를 읽어 request attribute에 넣고, Controller는 attribute 사용
  3) Cookie name을 고정하고(권장) path/secure/samesite 등만 설정으로 바꾸기
