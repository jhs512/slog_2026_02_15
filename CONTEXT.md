# 용어집 (CONTEXT)

이 저장소의 도메인 언어를 정의한다. 구현 세부는 담지 않는다 — 용어와 그 경계만.

## Post (글)

식별자 `id`(숫자, 예: `14300`)를 가진 마크다운 문서. 본문은 `content` 필드의 마크다운 문자열.

- **Raw** — Post의 `content` 마크다운 원문. `/p/{id}/raw` 가 `text/plain`으로 그대로 서빙한다.

### 글 상태 (Post Status)

`published` / `listed` / `title` 유무 조합으로 유도되는 3단계 표시 상태.

- **임시저장** — `!published`이면서 제목이 비었거나 센티널 제목 `"임시글"`인 글. (백엔드 temp API가 `"임시글"` 제목으로 생성하며, 작성/수정 API는 빈 제목을 거부하므로 실제로는 센티널 케이스가 대부분이다.)
- **비공개** — `!published` (제목 있음). 작성자만 볼 수 있는 글.
- **미노출** — `published && !listed`. 링크로는 열리지만 목록에는 나오지 않는 글.

"비공개"라는 말을 `listed=false`에 쓰지 않는다 — 그건 **미노출**이다.

## PPT

Post `content` 안에 임베드된 슬라이드 덱. `<details ppt-id="...">` 블록 하나가 하나의 PPT.

- **ppt-id** — 한 Post 안에서 PPT를 식별하는 문자열 (예: `"1"`, `"2"`).
- **슬라이드(Slide)** — PPT 마크다운을 `\n---\n` 구분자로 나눈 한 조각. URL 해시에서 1-index (`#1` = 첫 슬라이드).
- `/p/{id}/ppt/{pptId}` 가 해당 PPT를 슬라이드 단위로 **렌더링**한다.

## Raw 아티팩트 (Raw Artifact)

Post `content` 안에 임베드된, 그대로 꺼내 쓸 수 있는 단일 원본 파일. `<details raw-id="...">` 블록 하나가 하나의 Raw 아티팩트이며, 블록 안 **첫 fenced code block**이 그 페이로드다 (예: Postman Collection JSON).

- **raw-id** — 한 Post 안에서 Raw 아티팩트를 식별하는 문자열 (예: `"1"`). PPT의 ppt-id와 평행한 개념.
- `/p/{id}/raw/{rawId}` 가 페이로드를 **fence 없이(unwrap)** 꺼내, fenced 블록의 언어 태그에서 유도한 Content-Type으로 서빙한다 (` ```json ` → `application/json; charset=utf-8`). 그래서 외부 도구(Postman 등)가 URL로 바로 import 할 수 있다.
- PPT가 "임베드된 덱을 **렌더링**"이라면, Raw 아티팩트는 "임베드된 파일을 **원본 그대로 서빙**". 둘 다 `<details {kind}-id>` 패턴을 공유한다.

## surl

Post 마크다운 안에서 쓰는 내부 링크 약식 표기. 렌더 시 절대 경로로 변환된다.

- `surl:ppt/{id}` → `/p/{현재글}/ppt/{id}`
- `surl:raw/{id}` → `/p/{현재글}/raw/{id}`
- `surl:{postId}` / `surl:{postId}/ppt/{id}` / `surl:{postId}/raw/{id}` — 다른 글 대상 변형.
