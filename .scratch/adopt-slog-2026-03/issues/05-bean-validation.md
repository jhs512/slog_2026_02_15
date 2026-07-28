# 05 — Bean Validation 묶음

**What to build:** 잘못된 요청(음수 id, 범위 밖 페이징)이 조용히 보정되는 대신 400으로 거절되고, 생성 엔드포인트가 201을 반환한다.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] 컨트롤러 `@Validated` + 경로 변수 `@Positive` — 음수/0 id에 400
- [ ] 생성 엔드포인트 `@ResponseStatus(CREATED)` — 201 반환
- [ ] adm 페이징 `@Min/@Max` — 클램프 대신 400
- [ ] MockMvc 테스트로 400/201 검증, 기존 테스트의 상태 코드 기대값 갱신
