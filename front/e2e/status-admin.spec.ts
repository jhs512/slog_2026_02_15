import { Locator, Page, expect, test } from "@playwright/test";

import { storageStatePath } from "./accounts";
import { apiAs, createPost, uniqueTitle } from "./api";

// 글 상태 배지(/p/mine) + 관리자 회원 목록/검색(/adm/members)

test.describe("내 글 상태 배지", () => {
  // core.spec의 user1 임시글 흐름과 충돌하지 않도록 user2를 쓴다
  test.use({ storageState: storageStatePath("user2") });

  // 제목에 배지 문구(비공개 등)가 섞이지 않게 영문 접미사를 쓴다
  const rowFor = (page: Page, title: string): Locator =>
    page.locator("li").filter({ hasText: title });

  const badgeIn = (row: Locator, badgeText: string): Locator =>
    row.getByText(badgeText, { exact: true });

  test("비공개/미노출/공개 글이 각각 올바른 배지로 표시된다", async ({
    page,
  }) => {
    const prefix = uniqueTitle("e2e-status");
    const api = await apiAs("user2");
    await createPost(api, {
      title: `${prefix}-secret`,
      content: "비공개 상태 검증용 본문",
      published: false,
      listed: false,
    });
    await createPost(api, {
      title: `${prefix}-unlisted`,
      content: "미노출 상태 검증용 본문",
      published: true,
      listed: false,
    });
    await createPost(api, {
      title: `${prefix}-open`,
      content: "공개 상태 검증용 본문",
      published: true,
      listed: true,
    });
    await api.dispose();

    await page.goto("/p/mine");

    await expect(
      badgeIn(rowFor(page, `${prefix}-secret`), "비공개"),
    ).toBeVisible();
    await expect(
      badgeIn(rowFor(page, `${prefix}-unlisted`), "미노출"),
    ).toBeVisible();
    // 발행+노출 글은 정상(공개) 배지
    await expect(badgeIn(rowFor(page, `${prefix}-open`), "공개")).toBeVisible();
  });

  test("임시저장 글(temp API)이 임시저장 배지로 표시된다", async ({ page }) => {
    const api = await apiAs("user2");
    const res = await api.post("/post/api/v1/posts/temp");
    expect(res.ok(), await res.text()).toBeTruthy();
    await api.dispose();

    // 이전 실행들이 쌓여도 임시글은 검색으로 특정한다 (한 계정의 임시글은 하나로 재사용됨)
    await page.goto(`/p/mine?kw=${encodeURIComponent("임시글")}`);

    const tempRow = rowFor(page, "임시글").first();
    await expect(badgeIn(tempRow, "임시저장")).toBeVisible();
  });
});

test.describe("관리자 회원 목록", () => {
  test.use({ storageState: storageStatePath("admin") });

  test("회원 목록에 시드 회원이 표시된다", async ({ page }) => {
    await page.goto("/adm/members");

    await expect(page.getByText("회원 목록")).toBeVisible();
    await expect(page.getByText(/\d+ : user1 \/ 유저1/)).toBeVisible();
    await expect(page.getByText(/\d+ : admin \/ 관리자/)).toBeVisible();
  });

  test("kw 검색으로 회원이 필터링된다", async ({ page }) => {
    await page.goto("/adm/members");

    const searchInput = page.getByPlaceholder("검색어를 입력하세요");
    await searchInput.fill("user2");
    await searchInput.press("Enter");

    // 절대 개수("총 1명")를 단언하지 않는다 — 회원이 늘면 깨지는 취약한 기대값이다.
    // 검색어에 맞는 회원만 보이고 나머지는 안 보이는지로 필터링을 검증한다.
    await expect(page.getByText(/\d+ : user2 \/ 유저2/)).toBeVisible();
    await expect(page.getByText(/\d+ : user1 \//)).not.toBeVisible();
    await expect(page.getByText(/\d+ : admin \//)).not.toBeVisible();
  });
});
