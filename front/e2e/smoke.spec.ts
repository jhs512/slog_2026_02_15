import { expect, test } from "@playwright/test";

import { apiAs, createPost, uniqueTitle } from "./api";

// 하네스 관통 스모크: API로 글을 만들고, 비로그인 브라우저가 목록에서 그 글을 본다
test("API로 발행한 글이 목록에 보인다", async ({ page }) => {
  const api = await apiAs("user1");
  const title = uniqueTitle("e2e-smoke");
  await createPost(api, {
    title,
    content: "e2e 스모크 본문입니다.",
    published: true,
    listed: true,
  });
  await api.dispose();

  await page.goto("/p");
  await expect(page.getByText(title)).toBeVisible();
});
