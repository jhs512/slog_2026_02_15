/**
 * 코드 블록 문법(```)을 Toast UI Editor 커스텀 문법($$)으로 변환
 *
 * 지원 문법:
 * - ```uml, ```plantuml  → $$uml ... $$
 * - ```mermaid            → $$mermaid ... $$
 * - ```youtube            → $$youtube ... $$
 * - ```chart              → $$chart ... $$
 * - ```codepen            → $$codepen ... $$
 * - ```katex, ```math     → $$katex ... $$
 */
export function convertCodeBlocksToDiagramSyntax(content: string): string {
  // 변환 규칙: [매칭할 코드블록 언어들, 변환될 $$ 태그명]
  const rules: [RegExp, string][] = [
    [/```(?:uml|plantuml)\s*\n([\s\S]*?)```/gi, "uml"],
    [/```mermaid\s*\n([\s\S]*?)```/gi, "mermaid"],
    [/```youtube\s*\n([\s\S]*?)```/gi, "youtube"],
    [/```chart\s*\n([\s\S]*?)```/gi, "chart"],
    [/```codepen\s*\n([\s\S]*?)```/gi, "codepen"],
    [/```(?:katex|math)\s*\n([\s\S]*?)```/gi, "katex"],
  ];

  let result = content;
  for (const [pattern, tag] of rules) {
    result = result.replace(
      pattern,
      (_, code) => `$$${tag}\n${code.trim()}\n$$`,
    );
  }

  return result;
}

export function processMarkdownContent(
  content: string,
  currentPostId: string | number,
): string {
  let processedContent = content;

  // 1. [text](surl:ppt/ID) -> [text](/p/{currentPostId}/ppt/ID)
  // Support optional hash: surl:ppt/ID#HASH
  processedContent = processedContent.replace(
    /\[([^\]]+)\]\(surl:ppt\/([^)#\s]+)(?:#([^)]+))?\)/g,
    (_, text, id, hash) => {
      const hashPart = hash ? `#${hash}` : "";
      return `[${text}](/p/${currentPostId}/ppt/${id}${hashPart})`;
    },
  );

  // 2. [text](surl:POSTID/ppt/ID) -> [text](/p/{POSTID}/ppt/ID)
  // Support optional hash: surl:POSTID/ppt/ID#HASH
  processedContent = processedContent.replace(
    /\[([^\]]+)\]\(surl:(\d+)\/ppt\/([^)#\s]+)(?:#([^)]+))?\)/g,
    (_, text, postId, id, hash) => {
      const hashPart = hash ? `#${hash}` : "";
      return `[${text}](/p/${postId}/ppt/${id}${hashPart})`;
    },
  );

  // 3. [text](surl:raw/ID) -> [text](/p/{currentPostId}/raw/ID)
  processedContent = processedContent.replace(
    /\[([^\]]+)\]\(surl:raw\/([^)\s]+)\)/g,
    (_, text, id) => {
      return `[${text}](/p/${currentPostId}/raw/${id})`;
    },
  );

  // 4. [text](surl:POSTID/raw/ID) -> [text](/p/{POSTID}/raw/ID)
  processedContent = processedContent.replace(
    /\[([^\]]+)\]\(surl:(\d+)\/raw\/([^)\s]+)\)/g,
    (_, text, postId, id) => {
      return `[${text}](/p/${postId}/raw/${id})`;
    },
  );

  // 5. [text](surl:POSTID) or [text](surl:POSTID#HASH) -> [text](/p/{POSTID}#HASH)
  processedContent = processedContent.replace(
    /\[([^\]]+)\]\(surl:(\d+)(?:#([^)]+))?\)/g,
    (_, text, postId, hash) => {
      const hashPart = hash ? `#${hash}` : "";
      return `[${text}](/p/${postId}${hashPart})`;
    },
  );

  return processedContent;
}

export function stripMarkdown(input: string): string {
  // 1. $$...$$ 또는 ```...``` 내용을 제거
  const cleanedContent = input.replace(
    /(\$\$[\s\S]*?\$\$|```[\s\S]*?```)/g,
    "",
  );

  // 2. 마크다운 링크에서 텍스트만 추출 ([text](url) -> text)
  const withoutLinks = cleanedContent.replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");

  // 3. 영어, 소괄호, 한글(자음/모음 포함), 특수문자(:;/,〈〉=\-_[]), 띄워쓰기, 줄바꿈만 허용
  // 4. 연속된 공백과 줄바꿈을 하나의 공백으로 변경하고 앞뒤 공백 제거
  return withoutLinks
    .replace(/[^a-zA-Z가-힣ㄱ-ㅎㅏ-ㅣ0-9().?!:;/,〈〉=\-_\[\]\s]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 157)
    .replace(/(.{157}).*/, "$1...");
}

export function getSummaryFromContent(content: string): string {
  let summary = content;

  if (summary.startsWith("# 요약")) {
    const endIndex =
      summary.slice(1).search(/(\n\n|\#)/) !== -1
        ? summary.slice(1).search(/(\n\n|\#)/)
        : summary.length;

    if (endIndex !== -1) {
      summary = summary.slice(4, endIndex + 1).trim();
    }

    summary = summary
      .split("\n")
      .map((line) => line.replace(/^-\s*/, ""))
      .join("\n");

    return summary.trim();
  }

  return "";
}

/**
 * 코드 블록 원문을 HTML 이스케이프
 *
 * Toast UI 코드 하이라이트 플러그인은 Prism에 등록되지 않은 언어(```text,
 * 언어 없는 ```)일 때 원문을 그대로 `type: "html"` 로 내보낸다.
 * 그 결과 코드펜스 안의 `<s>` 같은 문자열이 실제 태그로 파싱된다.
 */
export function escapeHtml(input: string): string {
  return input
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// ─── <details> 구간을 $$details 커스텀 블록으로 감싸기 ───────

const RE_CODE_FENCE = /^ {0,3}(`{3,}|~{3,})/;
const RE_DETAILS_OPEN = /^ {0,3}<details\b[^>]*>\s*$/i;
const RE_DETAILS_CLOSE = /^ {0,3}<\/details>\s*$/i;

/**
 * `<details> ... </details>` 구간을 `$$details` 커스텀 블록 하나로 감싼다.
 *
 * CommonMark에서 원시 HTML 블록은 빈 줄을 만나면 끝난다. 그래서 `<summary>` 뒤에
 * 빈 줄을 두면 그 뒤 마크다운이 `<details>` 의 자식이 아니라 형제 노드가 되고,
 * Toast UI 뷰어는 루트 노드마다 `<div data-nodeid>` 로 감싸므로 `<details>` 가
 * 거기서 닫혀버린다. 결과적으로 내용이 토글 밖으로 빠져나간다.
 *
 * 구간 전체를 커스텀 블록 하나로 만들면 루트 노드가 하나가 되어 중첩이 유지된다.
 * 커스텀 블록은 `$$` 만 있는 줄에서 닫히는데, 안쪽 `$$mermaid ... $$` 같은
 * 블록이 먼저 닫아버리는 것을 막기 위해 원문을 URI 인코딩해 한 줄로 넣는다.
 *
 * 코드펜스 안의 `<details>` 는 건드리지 않고, 닫히지 않은 `<details>` 는
 * 원문을 그대로 둔다.
 */
export function wrapDetailsBlocks(content: string): string {
  const lines = content.split("\n");
  const regions: [number, number][] = [];

  let fence: string | null = null;
  let depth = 0;
  let start = -1;

  lines.forEach((line, index) => {
    const fenceMatch = line.match(RE_CODE_FENCE);

    if (fence) {
      const marker = fenceMatch?.[1];
      if (marker && marker[0] === fence[0] && marker.length >= fence.length) {
        fence = null;
      }
      return;
    }

    if (fenceMatch) {
      fence = fenceMatch[1];
      return;
    }

    if (RE_DETAILS_OPEN.test(line)) {
      if (depth === 0) start = index;
      depth++;
      return;
    }

    if (depth > 0 && RE_DETAILS_CLOSE.test(line)) {
      depth--;
      if (depth === 0) regions.push([start, index]);
    }
  });

  if (regions.length === 0) return content;

  const result: string[] = [];
  let cursor = 0;

  for (const [regionStart, regionEnd] of regions) {
    result.push(...lines.slice(cursor, regionStart));
    result.push("$$details");
    result.push(
      encodeURIComponent(lines.slice(regionStart, regionEnd + 1).join("\n")),
    );
    result.push("$$");
    cursor = regionEnd + 1;
  }

  result.push(...lines.slice(cursor));

  return result.join("\n");
}

/** `$$details` 커스텀 블록 본문(URI 인코딩된 원문)을 되돌린다. */
export function decodeDetailsBlock(literal: string): string {
  try {
    return decodeURIComponent(literal.trim());
  } catch {
    return literal;
  }
}
