# 06 — 설정 구조 개선

**What to build:** 신규 개발자가 환경변수 없이 dev 프로필을 기동할 수 있고, 로컬 테스트가 dev Redis 세션을 지우지 않으며, DB명이 한 곳에서 파생된다.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] `dbBaseName` 프로퍼티에서 dev/test DB명 파생
- [ ] dev/test Redis database 분리 (dev 0 / test 1)
- [ ] JWT 시크릿·시스템 API 키에 dev용 기본값 — 환경변수 없이 기동 가능
- [ ] 공통 ddl-auto 제거, 프로필별 이관 + dev 전용 yaml 신설 (unlogged 테이블 dialect 포함)
- [ ] CustomConfigProperties immutable 생성자 바인딩 + @ConfigurationPropertiesScan
- [ ] 기동(dev)·전체 테스트(test) 정상 동작
