"use client";

import "@toast-ui/editor/dist/toastui-editor.css";
import "@toast-ui/editor/dist/theme/toastui-editor-dark.css";

import chartPlugin from "@toast-ui/editor-plugin-chart";
// @ts-expect-error - 타입 정보 없음
import codeSyntaxHighlight from "@toast-ui/editor-plugin-code-syntax-highlight/dist/toastui-editor-plugin-code-syntax-highlight-all";
import tableMergedCell from "@toast-ui/editor-plugin-table-merged-cell";

import "@toast-ui/chart/dist/toastui-chart.css";
import "@toast-ui/editor-plugin-table-merged-cell/dist/toastui-editor-plugin-table-merged-cell.css";

import { forwardRef, useEffect, useMemo, useRef } from "react";

// @ts-expect-error - 타입 정보 없음. <details> 안쪽 마크다운을 별도 렌더할 때 쓰는
// Viewer 클래스 본체 (@toast-ui/react-editor 의 Viewer 가 감싸고 있는 것과 동일)
import ViewerClass from "@toast-ui/editor/dist/toastui-editor-viewer";
import { Viewer } from "@toast-ui/react-editor";

import {
  convertCodeBlocksToDiagramSyntax,
  decodeDetailsBlock,
  escapeHtml,
  processMarkdownContent,
  wrapDetailsBlocks,
} from "../markdownUtils";
import { filterObjectKeys, getParamsFromUrl, isExternalUrl } from "../utils";

// ─── PlantUML 인코딩 함수 ───────────────────────────────
function encodePlantUML(text: string): string {
  // PlantUML uses a custom encoding: deflate -> base64 variant
  // 간단한 방식: hex encoding 사용 (~h prefix)
  const hex = Array.from(new TextEncoder().encode(text))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
  return "~h" + hex;
}

// ─── Mermaid 인코딩 함수 (mermaid.ink 서버용) ───────────
function encodeMermaid(text: string): string {
  const json = JSON.stringify({
    code: text,
    mermaid: { theme: "default" },
  });
  const bytes = new TextEncoder().encode(json);
  const binary = Array.from(bytes)
    .map((b) => String.fromCharCode(b))
    .join("");
  return btoa(binary);
}

// ─── 플러그인: $$uml ... $$ → PlantUML 이미지 ──────────
function umlPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    uml(node: any) {
      const encoded = encodePlantUML(node.literal);
      const imgUrl = `https://www.plantuml.com/plantuml/svg/${encoded}`;
      return [
        {
          type: "openTag",
          tagName: "div",
          outerNewLine: true,
          attributes: { class: "diagram-container my-4" },
        },
        {
          type: "html",
          content: `<img src="${imgUrl}" alt="PlantUML Diagram" style="max-width: 100%;" />`,
        },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: $$mermaid ... $$ → Mermaid 이미지 ────────
// mermaid.ink SVG 버그 보정:
// foreignObject의 width가 실제 텍스트보다 좁게 계산되어 마지막 글자가 잘리는 문제를
// SVG fetch → foreignObject width 패치로 해결
function resolveMermaidWidths() {
  if (typeof document === "undefined") return;
  const imgs = document.querySelectorAll<HTMLImageElement>(
    "img[data-mermaid-png]",
  );
  imgs.forEach(async (svgImg) => {
    if (svgImg.dataset.mermaidResolved) return;
    svgImg.dataset.mermaidResolved = "1";

    try {
      const resp = await fetch(svgImg.src);
      let svgText = await resp.text();
      const viewBoxMatch = svgText.match(/viewBox="([^"]+)"/);
      if (!viewBoxMatch) throw new Error("no viewBox");

      const [, , vbW] = viewBoxMatch[1].split(/\s+/).map(Number);

      // 1) SVG inline max-width 제거 (중앙정렬 시 치우침 방지)
      svgText = svgText.replace(
        /style="[^"]*max-width:\s*[\d.]+px;?[^"]*"/,
        'style=""',
      );

      // 2) nodeLabel foreignObject width가 텍스트보다 좁은 버그 보정
      // width를 4px 넓혀서 마지막 글자가 잘리지 않도록 함
      const foWidthBump = 4;
      svgText = svgText.replace(
        /<foreignObject\s+width="([\d.]+)"\s+height="([\d.]+)">/g,
        (match, w, h) => {
          const nw = parseFloat(w);
          if (nw > 0) {
            return `<foreignObject width="${nw + foWidthBump}" height="${h}">`;
          }
          return match;
        },
      );

      const blob = new Blob([svgText], { type: "image/svg+xml" });
      const prevSrc = svgImg.src;
      svgImg.src = URL.createObjectURL(blob);
      if (prevSrc.startsWith("blob:")) URL.revokeObjectURL(prevSrc);
      svgImg.width = Math.round(vbW);
    } catch {
      // SVG fetch/파싱 실패 시 PNG 프로브로 폴백
      const pngUrl = svgImg.dataset.mermaidPng;
      if (pngUrl) {
        const probe = new Image();
        probe.onload = () => {
          svgImg.width = probe.naturalWidth;
        };
        probe.src = pngUrl;
      }
    }
  });
}

function mermaidPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mermaid(node: any) {
      const encoded = encodeMermaid(node.literal);
      const svgUrl = `https://mermaid.ink/svg/${encoded}`;
      const pngUrl = `https://mermaid.ink/img/${encoded}`;
      return [
        {
          type: "openTag",
          tagName: "div",
          outerNewLine: true,
          attributes: { class: "diagram-container my-4" },
        },
        {
          type: "html",
          content: `<img src="${svgUrl}" alt="Mermaid Diagram" data-mermaid-png="${pngUrl}" width="500" style="max-width: 100%; max-height: 100%;" />`,
        },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: $$youtube ... $$ → YouTube 임베드 ────────
// (원본 slog_2025_04에서 포팅, URL 파라미터 지원: margin-left, margin-right, max-width)
function youtubePlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    youtube(node: any) {
      const html = renderYoutube(node.literal);
      return [
        { type: "openTag", tagName: "div", outerNewLine: true },
        { type: "html", content: html },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  function renderYoutube(url: string) {
    url = url.replace("https://www.youtube.com/watch?v=", "");
    url = url.replace("http://www.youtube.com/watch?v=", "");
    url = url.replace("www.youtube.com/watch?v=", "");
    url = url.replace("youtube.com/watch?v=", "");
    url = url.replace("https://youtu.be/", "");
    url = url.replace("http://youtu.be/", "");
    url = url.replace("youtu.be/", "");

    const urlParams = getParamsFromUrl(url);

    const ratio = "aspect-[16/9]";
    let marginLeft = "auto";

    if (urlParams["margin-left"]) {
      marginLeft = urlParams["margin-left"];
    }

    let marginRight = "auto";

    if (urlParams["margin-right"]) {
      marginRight = urlParams["margin-right"];
    }

    let maxWidth = "800";
    if (urlParams["max-width"]) {
      maxWidth = urlParams["max-width"];
    }

    let youtubeId = url;

    if (youtubeId.indexOf("?") !== -1) {
      const pos = url.indexOf("?");
      youtubeId = youtubeId.substring(0, pos);
    }

    return (
      '<div style="max-width:' +
      maxWidth +
      "px; margin-left:" +
      marginLeft +
      "; margin-right:" +
      marginRight +
      ';" class="' +
      ratio +
      ' relative my-4"><iframe class="absolute top-0 left-0 w-full h-full" src="https://www.youtube.com/embed/' +
      youtubeId +
      '" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe></div>'
    );
  }

  return { toHTMLRenderers };
}

// ─── 플러그인: $$codepen ... $$ → CodePen 임베드 ────────
// (원본 slog_2025_04에서 포팅, URL 파라미터 지원: height, width)
function codepenPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    codepen(node: any) {
      const html = renderCodepen(node.literal);
      return [
        { type: "openTag", tagName: "div", outerNewLine: true },
        { type: "html", content: html },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  function renderCodepen(url: string) {
    const urlParams = getParamsFromUrl(url);

    let height = "400";

    if (urlParams.height) {
      height = urlParams.height;
    }

    let width = "100%";

    if (urlParams.width) {
      width = urlParams.width;
    }

    if (!width.includes("px") && !width.includes("%")) {
      width += "px";
    }

    let iframeUri = url;

    if (iframeUri.indexOf("#") !== -1) {
      const pos = iframeUri.indexOf("#");
      iframeUri = iframeUri.substring(0, pos);
    }

    return (
      '<iframe class="my-4" height="' +
      height +
      '" style="width: ' +
      width +
      ';" title="" src="' +
      iframeUri +
      '" allowtransparency="true" allowfullscreen="true"></iframe>'
    );
  }
  return { toHTMLRenderers };
}

// ─── 플러그인: $$katex ... $$ → 수식 이미지 ────────────
// KaTeX 서버사이드 렌더링 (외부 의존성 없이 이미지로 처리)
function katexPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    katex(node: any) {
      const expression = (node.literal || "").trim();
      const encoded = encodeURIComponent(expression);
      const imgUrl = `https://math.vercel.app?from=${encoded}`;
      return [
        {
          type: "openTag",
          tagName: "div",
          outerNewLine: true,
          attributes: { class: "diagram-container katex-container my-4" },
        },
        {
          type: "html",
          content: `<img src="${imgUrl}" alt="Math: ${expression.replace(/"/g, "&quot;").slice(0, 100)}" style="max-width: 100%;" />`,
        },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: $$hide ... $$ → 콘텐츠 숨김 ─────────────
function hidePlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars
    hide(node: any) {
      return [
        { type: "openTag", tagName: "div", outerNewLine: true },
        { type: "html", content: "" },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: $$ppt ... $$ → PPT 블록 숨김 ────────────
function pptPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars
    ppt(node: any) {
      return [
        { type: "openTag", tagName: "div", outerNewLine: true },
        { type: "html", content: "" },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: $$config ... $$ → 설정 블록 숨김 ─────────
function configPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars
    config(node: any) {
      return [
        { type: "openTag", tagName: "div", outerNewLine: true },
        { type: "html", content: "" },
        { type: "closeTag", tagName: "div", outerNewLine: true },
      ];
    },
  };

  return { toHTMLRenderers };
}

// ─── 플러그인: 코드 하이라이트 (비하이라이팅 펜스 이스케이프 보강) ──
// 공식 code-syntax-highlight 플러그인은 Prism에 등록되지 않은 언어(```text, 언어
// 없는 ```)일 때 코드 원문을 이스케이프 없이 `type: "html"` 로 내보낸다.
// 그래서 펜스 안의 `<s>` 같은 문자열이 진짜 태그로 파싱돼 문서 뒷부분 전체를
// 오염시킨다. 하이라이팅을 타지 않은 경우(출력 === 원문)에만 이스케이프한다.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function codeSyntaxHighlightEscaped(context: any, options: any) {
  const plugin = codeSyntaxHighlight(context, options);
  const originalCodeBlock = plugin?.toHTMLRenderers?.codeBlock;

  if (!originalCodeBlock) return plugin;

  return {
    ...plugin,
    toHTMLRenderers: {
      ...plugin.toHTMLRenderers,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      codeBlock(node: any, rendererContext: any) {
        const tokens = originalCodeBlock(node, rendererContext);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return tokens.map((token: any) =>
          token.type === "html" && token.content === node.literal
            ? { ...token, content: escapeHtml(node.literal) }
            : token,
        );
      },
    },
  };
}

// ─── 플러그인: $$details ... $$ → 접이식 블록 ──────────
// wrapDetailsBlocks 가 감싼 구간을 받아, 안쪽 마크다운을 별도 Viewer 로 렌더한 뒤
// <details> 하나로 합친다. <summary> 는 직접 자식이어야 토글이 동작하므로
// 중첩 렌더에 맡기지 않고 원문에서 떼어내 그대로 붙인다.
function renderMarkdownFragment(markdown: string): string {
  if (typeof document === "undefined") return "";

  // 화면 밖에 붙였다 떼는 이유: chart 처럼 레이아웃을 재는 플러그인이
  // 문서에 붙지 않은 노드에서는 크기를 0으로 계산한다.
  const host = document.createElement("div");
  host.style.cssText = "position:absolute;left:-9999px;top:0;width:800px;";
  document.body.appendChild(host);

  try {
    new ViewerClass({
      el: host,
      initialValue: markdown,
      plugins: viewerPlugins,
      customHTMLRenderer: viewerCustomHTMLRenderer,
    });
    return host.querySelector(".toastui-editor-contents")?.innerHTML ?? "";
  } finally {
    host.remove();
  }
}

function renderDetailsBlock(literal: string): string {
  const source = decodeDetailsBlock(literal);
  const match = source.match(
    /^\s*<details([^>]*)>\s*\n?([\s\S]*?)\n?\s*<\/details>\s*$/i,
  );

  if (!match) return escapeHtml(source);

  const [, attributes, body] = match;
  const summaryMatch = body.match(
    /^\s*(<summary[^>]*>[\s\S]*?<\/summary>)\s*/i,
  );
  const summary = summaryMatch ? summaryMatch[1] : "";
  const inner = summaryMatch ? body.slice(summaryMatch[0].length) : body;
  // 안쪽에 또 <details> 가 있으면 같은 처리를 재귀로 받게 한다
  const innerHTML = renderMarkdownFragment(wrapDetailsBlocks(inner));

  return `<details${attributes}>${summary}${innerHTML}</details>`;
}

function detailsPlugin() {
  const toHTMLRenderers = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    details(node: any) {
      return [{ type: "html", content: renderDetailsBlock(node.literal) }];
    },
  };

  return { toHTMLRenderers };
}

// ─── Viewer 공통 설정 ───────────────────────────────────
// 중첩 렌더(renderMarkdownFragment)도 같은 설정을 써야 하므로 모듈 스코프에 둔다.

const viewerCustomHTMLRenderer = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  heading(node: any, { entering, getChildrenText }: any) {
    return {
      type: entering ? "openTag" : "closeTag",
      tagName: `h${node.level}`,
      attributes: {
        id: getChildrenText(node).trim().replaceAll(" ", "-"),
      },
    };
  },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  link(node: any, { entering }: any) {
    return {
      type: entering ? "openTag" : "closeTag",
      tagName: `a`,
      attributes: {
        href: node.destination,
        target: isExternalUrl(node.destination) ? "_blank" : "_self",
      },
    };
  },
  htmlBlock: {
    // iframe 속성 필터링 (보안)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    iframe(node: any) {
      const newAttrs = filterObjectKeys(node.attrs, [
        "src",
        "width",
        "height",
        "allow",
        "allowfullscreen",
        "frameborder",
        "scrolling",
        "class",
      ]);
      return [
        {
          type: "openTag",
          tagName: "iframe",
          outerNewLine: true,
          attributes: newAttrs,
        },
        { type: "html", content: node.childrenHTML },
        { type: "closeTag", tagName: "iframe", outerNewLine: false },
      ];
    },
  },
};

const viewerPlugins = [
  youtubePlugin,
  codepenPlugin,
  katexPlugin,
  umlPlugin,
  mermaidPlugin,
  hidePlugin,
  pptPlugin,
  configPlugin,
  detailsPlugin,
  [
    chartPlugin,
    {
      minWidth: 100,
      maxWidth: 800,
      minHeight: 100,
      maxHeight: 400,
    },
  ],
  codeSyntaxHighlightEscaped,
  tableMergedCell,
];

// ─── 컴포넌트 ───────────────────────────────────────────

export interface ToastUIEditorViewerCoreProps {
  initialValue: string;
  theme: "dark" | "light";
  postId?: string | number;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ToastUIEditorViewerCore = forwardRef<any, ToastUIEditorViewerCoreProps>(
  (props, ref) => {
    // 1. 코드 블록 다이어그램 문법 변환 (```uml -> $$uml$$, ```youtube -> $$youtube$$ 등)
    // 2. surl: 링크 처리
    // 3. <details> 구간을 $$details 커스텀 블록 하나로 묶기
    const processedContent = useMemo(() => {
      let content = convertCodeBlocksToDiagramSyntax(props.initialValue);
      if (props.postId) {
        content = processMarkdownContent(content, props.postId);
      }
      return wrapDetailsBlocks(content);
    }, [props.initialValue, props.postId]);

    // Mermaid SVG에 PNG 기반 실제 너비 세팅
    useEffect(() => {
      // Viewer 렌더링 후 약간의 딜레이를 주어 DOM에 img가 생긴 뒤 실행
      const timer = setTimeout(resolveMermaidWidths, 100);
      return () => clearTimeout(timer);
    }, [processedContent]);

    // ssr:false 로 로드되는 Viewer는 URL 해시 진입 시점에 DOM이 없어서
    // 브라우저 기본 해시 스크롤이 동작하지 않음 → viewer 렌더 후 수동 스크롤.
    // 단, 실시간 업데이트로 processedContent가 바뀔 때마다 재스크롤되면 안 되므로
    // 최초 진입 1회만 수행한다.
    const hasScrolledToHashRef = useRef(false);
    useEffect(() => {
      if (typeof window === "undefined") return;
      if (hasScrolledToHashRef.current) return;
      const hash = window.location.hash;
      if (!hash) return;

      const id = decodeURIComponent(hash.slice(1));
      const timer = setTimeout(() => {
        const el =
          document.getElementById(id) ??
          document.querySelector(`[id^="${CSS.escape(id)}"]`);
        if (el) {
          el.scrollIntoView();
          hasScrolledToHashRef.current = true;
        }
      }, 100);
      return () => clearTimeout(timer);
    }, [processedContent]);

    return (
      <Viewer
        theme={props.theme}
        plugins={viewerPlugins}
        ref={ref}
        initialValue={processedContent}
        customHTMLRenderer={viewerCustomHTMLRenderer}
      />
    );
  },
);

ToastUIEditorViewerCore.displayName = "ToastUIEditorViewerCore";

export default ToastUIEditorViewerCore;
