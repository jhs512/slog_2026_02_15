import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

async function getPost(id: number) {
  const cookieStore = await cookies();
  const response = await fetch(`${API_BASE_URL}/post/api/v1/posts/${id}`, {
    headers: {
      cookie: cookieStore.toString(),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return response.json();
}

// <details raw-id="..."> 블록 안 내용 추출 (PPT extractPptContent 미러링)
function extractRawBlock(content: string, rawId: string): string | null {
  const escapedId = rawId.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

  // SLOG 방식: <div markdown="1"> 안에 내용
  const patternWithDiv = new RegExp(
    `<details[^>]*\\braw-id\\s*=\\s*["']${escapedId}["'][^>]*>[\\s\\S]*?<summary[^>]*>[\\s\\S]*?</summary>[\\s\\S]*?<div[^>]*markdown=["']1["'][^>]*>([\\s\\S]*?)</div>`,
    "i",
  );
  let match = content.match(patternWithDiv);

  // 기본 방식: details 내 직접 내용
  if (!match) {
    const patternDirect = new RegExp(
      `<details[^>]*\\braw-id\\s*=\\s*["']${escapedId}["'][^>]*>[\\s\\S]*?<summary[^>]*>[^<]*</summary>([\\s\\S]*?)</details>`,
      "i",
    );
    match = content.match(patternDirect);
  }

  return match ? match[1] : null;
}

// 블록 안 첫 fenced code block의 언어 + 안쪽 내용 (fence 제거)
function extractFence(block: string): { lang: string; body: string } | null {
  const match = block.match(/```([a-zA-Z0-9]*)[^\n]*\n([\s\S]*?)\n```/);
  if (!match) return null;
  return { lang: match[1].toLowerCase(), body: match[2] };
}

// fenced 블록 언어 → Content-Type
const CONTENT_TYPE_BY_LANG: Record<string, string> = {
  json: "application/json; charset=utf-8",
  yaml: "application/yaml; charset=utf-8",
  yml: "application/yaml; charset=utf-8",
  xml: "application/xml; charset=utf-8",
  html: "text/html; charset=utf-8",
  csv: "text/csv; charset=utf-8",
};

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string; rawId: string }> },
) {
  const { id: idStr, rawId } = await params;
  const id = parseInt(idStr);

  const post = await getPost(id);

  if (!post) {
    return NextResponse.json({ error: "Post not found" }, { status: 404 });
  }

  const block = extractRawBlock(post.content, rawId);

  if (block === null) {
    return NextResponse.json(
      { error: `Raw block not found: ${rawId}` },
      { status: 404 },
    );
  }

  const fence = extractFence(block);

  if (!fence) {
    return NextResponse.json(
      { error: `No fenced code block in raw block: ${rawId}` },
      { status: 404 },
    );
  }

  const contentType =
    CONTENT_TYPE_BY_LANG[fence.lang] ?? "text/plain; charset=utf-8";

  return new NextResponse(fence.body, {
    status: 200,
    headers: {
      "Content-Type": contentType,
    },
  });
}
