# 04 — 특수 라우트 시나리오

**What to build:** 이 서비스 고유 라우트들이 브라우저/HTTP 레벨에서 검증된다: PPT 슬라이드 렌더·해시 이동, raw 아티팩트의 Content-Type 서빙, 해시 앵커 스크롤, 구 URL 리다이렉트.

**Blocked by:** 01 — E2E 하네스 부트스트랩

**Status:** done

- [ ] PPT: `<details ppt-id>` 블록이 있는 글을 API로 생성 → `/p/{id}/ppt/{pptId}`에서 슬라이드 렌더, `#2` 해시로 두 번째 슬라이드 이동
- [ ] raw: ```json fenced 블록의 raw 아티팩트 생성 → `/p/{id}/raw/{rawId}` 응답이 `application/json` Content-Type + fence 없는 페이로드
- [ ] 해시 앵커: `#제목` URL로 진입 시 해당 헤딩으로 스크롤되고 sticky 헤더에 가려지지 않음 (특수문자 제목 포함)
- [ ] 리다이렉트: `/ken/{id}` → `/p/{id}` 이동 확인
- [ ] 용어는 CONTEXT.md(Raw 아티팩트, ppt-id, 슬라이드) 그대로 사용
