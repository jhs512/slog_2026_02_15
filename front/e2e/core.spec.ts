import { expect, test } from "@playwright/test";

import { ACCOUNTS, storageStatePath } from "./accounts";
import { apiAs, createPost, uniqueTitle } from "./api";

// 코어 스모크: 사이트의 본줄기 사용자 여정 (목록→뷰어, 관리자 로그인, 작성→발행, 댓글)

test("목록에서 글을 클릭하면 뷰어에 제목과 본문이 렌더된다", async ({
  page,
}) => {
  const title = uniqueTitle("e2e-core-view");
  const body = `뷰어 본문 검증용 텍스트 ${title}`;

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content: body,
    published: true,
    listed: true,
  });
  await api.dispose();

  await page.goto("/p");
  await page.getByRole("link", { name: title }).click();

  await expect(page).toHaveURL(new RegExp(`/p/${id}$`));
  await expect(
    page.getByRole("heading", { level: 1, name: title }),
  ).toBeVisible();
  await expect(page.getByText(body)).toBeVisible();
});

test("관리자 로그인 페이지에서 admin 계정으로 로그인할 수 있다", async ({
  page,
}) => {
  // storageState 미사용 — 로그인 UI 플로우 자체를 검증한다
  await page.goto("/adm/members/login");

  await expect(page.getByText("관리자 로그인")).toBeVisible();

  await page
    .getByPlaceholder("아이디를 입력하세요")
    .fill(ACCOUNTS.admin.username);
  await page
    .getByPlaceholder("비밀번호를 입력하세요")
    .fill(ACCOUNTS.admin.password);
  await page.getByRole("button", { name: "로그인" }).click();

  // 로그인 성공 → 홈으로 이동, 푸터가 관리자 메뉴로 바뀐다
  await expect(
    page.locator("footer").getByRole("link", { name: "관리자 메뉴" }),
  ).toBeVisible();
  await expect(page).toHaveURL(new RegExp("/$"));
});

test.describe("로그인 사용자 (user1)", () => {
  test.use({ storageState: storageStatePath("user1") });

  test("글 작성 화면에서 발행하면 목록에 노출된다", async ({ page }) => {
    const title = uniqueTitle("e2e-core-write");
    const body = `작성 플로우 검증용 본문 ${title}`;

    // /p/write는 임시글을 만들고 편집 화면으로 리다이렉트한다
    await page.goto("/p/write");
    await expect(page).toHaveURL(/\/p\/\d+\/edit$/);

    // 임시글 로딩(폼 reset)이 끝난 뒤 입력해야 값이 덮이지 않는다
    const titleInput = page.getByPlaceholder("제목을 입력하세요");
    await expect(titleInput).toHaveValue("임시글");

    await titleInput.fill(title);
    await page.getByPlaceholder("내용을 입력하세요").fill(body);
    await page.getByRole("checkbox", { name: "미노출/공개" }).check();
    await page.getByRole("checkbox", { name: "목록 노출" }).check();
    await page.getByRole("button", { name: "저장" }).click();

    await expect(page.getByText(/글이 수정되었습니다/)).toBeVisible();

    await page.goto("/p");
    await expect(page.getByText(title)).toBeVisible();
  });

  test("댓글을 작성하면 표시되고 삭제하면 사라진다", async ({ page }) => {
    const title = uniqueTitle("e2e-core-comment");
    const api = await apiAs("user1");
    const id = await createPost(api, {
      title,
      content: "댓글 플로우 검증용 본문",
      published: true,
      listed: false,
    });
    await api.dispose();

    const commentText = `e2e 댓글 ${uniqueTitle("내용")}`;

    await page.goto(`/p/${id}`);

    // 댓글 목록 로딩이 끝난 뒤 작성해야 화면에 반영된다
    await expect(page.getByText(/개의 댓글/)).toBeVisible();

    await page.getByPlaceholder("댓글을 작성해주세요...").fill(commentText);
    // 헤더에도 "작성" 버튼(새 글 작성)이 있으므로 본문 영역으로 한정한다
    await page.getByRole("main").getByRole("button", { name: "작성" }).click();

    await expect(page.getByText(commentText)).toBeVisible();

    await page.getByRole("button", { name: "댓글 삭제" }).click();
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: "삭제", exact: true })
      .click();

    await expect(page.getByText(commentText)).not.toBeVisible();
  });
});
