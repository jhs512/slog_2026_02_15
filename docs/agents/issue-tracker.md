# 이슈 트래커: GitHub

이 저장소의 이슈와 PRD는 GitHub 이슈로 관리합니다. 모든 작업은 `gh` CLI를 사용하세요.

## 규칙

- **이슈 생성**: `gh issue create --title "..." --body "..."`. 여러 줄 본문에는 heredoc을 사용하세요.
- **이슈 조회**: `gh issue view <number> --comments`. `jq`로 댓글을 필터링하고 라벨도 함께 가져옵니다.
- **이슈 목록**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` 에 적절한 `--label`, `--state` 필터를 추가합니다.
- **이슈에 댓글**: `gh issue comment <number> --body "..."`
- **라벨 추가 / 제거**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **닫기**: `gh issue close <number> --comment "..."`

저장소는 `git remote -v` 로 추론합니다 — 클론 안에서 실행하면 `gh` 가 자동으로 처리합니다.

## 스킬이 "이슈 트래커에 발행"하라고 할 때

GitHub 이슈를 생성합니다.

## 스킬이 "관련 티켓을 가져오라"고 할 때

`gh issue view <number> --comments` 를 실행합니다.
