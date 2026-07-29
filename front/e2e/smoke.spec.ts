import { expect, request, test } from "@playwright/test";

import { API_BASE, storageStatePath } from "./accounts";

// 하네스 관통 스모크: API로 글을 만들고, 비로그인 브라우저가 목록에서 그 글을 본다
test("API로 발행한 글이 목록에 보인다", async ({ page }) => {
  const api = await request.newContext({
    baseURL: API_BASE,
    storageState: storageStatePath("user1"),
  });

  const title = `e2e-smoke-${Date.now()}`;
  const res = await api.post("/post/api/v1/posts", {
    data: {
      title,
      content: "e2e 스모크 본문입니다.",
      published: true,
      listed: true,
    },
  });
  expect(res.status(), await res.text()).toBe(201);
  await api.dispose();

  await page.goto("/p");
  await expect(page.getByText(title)).toBeVisible();
});
