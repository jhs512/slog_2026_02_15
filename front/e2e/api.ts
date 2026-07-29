import { APIRequestContext, expect, request } from "@playwright/test";

import { API_BASE, AccountKey, storageStatePath } from "./accounts";

// 시드 계정으로 로그인된 API request context (setup 프로젝트가 만든 storageState 재사용)
export async function apiAs(key: AccountKey): Promise<APIRequestContext> {
  return request.newContext({
    baseURL: API_BASE,
    storageState: storageStatePath(key),
  });
}

// 비로그인 API request context (Origin 헤더 없음 — 조회수 hit 등에 사용)
export async function apiAnon(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: API_BASE });
}

export interface CreatePostOptions {
  title: string;
  content: string;
  published: boolean;
  listed: boolean;
}

// API로 글을 생성하고 id를 반환한다 — 테스트는 시드 글에 의존하지 않는다
export async function createPost(
  api: APIRequestContext,
  options: CreatePostOptions,
): Promise<number> {
  const res = await api.post("/post/api/v1/posts", { data: options });
  expect(res.status(), await res.text()).toBe(201);
  const body = await res.json();
  return body.data.id as number;
}

// 병렬 실행 대비: 테스트마다 충돌 없는 고유 제목
export function uniqueTitle(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
}
