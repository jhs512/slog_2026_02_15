import { expect, test } from "@playwright/test";

import { apiAs, createPost, uniqueTitle } from "./api";

// 특수 라우트: PPT 슬라이드 렌더/해시 이동, Raw 아티팩트 Content-Type 서빙, 해시 앵커 스크롤, /ken 리다이렉트

test("PPT 라우트가 슬라이드를 렌더하고 #2 해시로 두 번째 슬라이드로 이동한다", async ({
  page,
}) => {
  const title = uniqueTitle("e2e-ppt");
  const slide1Text = `첫 슬라이드 텍스트 ${title}`;
  const slide2Text = `둘째 슬라이드 텍스트 ${title}`;

  const content = [
    `# ${title}`,
    "",
    '<details ppt-id="1">',
    "<summary>e2e 슬라이드 덱</summary>",
    "",
    slide1Text,
    "",
    "---",
    "",
    slide2Text,
    "",
    "</details>",
  ].join("\n");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content,
    published: true,
    listed: false,
  });
  await api.dispose();

  await page.goto(`/p/${id}/ppt/1`);
  await expect(page.getByText(slide1Text)).toBeVisible();
  await expect(page.getByText(slide2Text)).not.toBeVisible();

  await page.goto(`/p/${id}/ppt/1#2`);
  await expect(page.getByText(slide2Text)).toBeVisible();
  await expect(page.getByText(slide1Text)).not.toBeVisible();
});

test("Raw 아티팩트가 언어 태그에서 유도된 Content-Type으로 fence 없이 서빙된다", async ({
  request,
}) => {
  const title = uniqueTitle("e2e-raw");
  const payload = `{\n  "name": "e2e-raw",\n  "value": 1\n}`;

  const content = [
    `# ${title}`,
    "",
    '<details raw-id="1">',
    "<summary>e2e Raw 아티팩트</summary>",
    "",
    "```json",
    payload,
    "```",
    "",
    "</details>",
  ].join("\n");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content,
    published: true,
    listed: false,
  });
  await api.dispose();

  const res = await request.get(`/p/${id}/raw/1`);
  expect(res.status()).toBe(200);
  expect(res.headers()["content-type"]).toBe("application/json; charset=utf-8");

  const body = await res.text();
  expect(body).toBe(payload);
  expect(body).not.toContain("```");
});

test.describe("해시 앵커 진입", () => {
  // 해딩 앞에 충분한 본문을 둬서 스크롤 없이는 해딩이 안 보이게 만든다
  const filler = Array.from(
    { length: 40 },
    (_, i) => `본문 채움 문단 ${i + 1}번째입니다.`,
  ).join("\n\n");

  async function createPostWithHeading(headingText: string) {
    const title = uniqueTitle("e2e-anchor");
    const content = `# ${title}\n\n${filler}\n\n## ${headingText}\n\n앵커 아래 본문입니다.\n\n${filler}`;

    const api = await apiAs("user1");
    const id = await createPost(api, {
      title,
      content,
      published: true,
      listed: false,
    });
    await api.dispose();
    return id;
  }

  for (const headingText of ["목표 지점 헤딩", "특수문자! (테스트) & 끝?"]) {
    test(`#해딩 해시로 진입하면 "${headingText}" 헤딩이 보이고 sticky 헤더에 가리지 않는다`, async ({
      page,
    }) => {
      const id = await createPostWithHeading(headingText);

      // 뷰어의 해딩 id 규칙: 텍스트의 공백을 "-"로 치환
      const anchorId = headingText.replaceAll(" ", "-");
      await page.goto(`/p/${id}#${encodeURIComponent(anchorId)}`);

      const heading = page.getByRole("heading", { name: headingText });
      await expect(heading).toBeInViewport();

      // sticky 헤더 아래에 있어야 한다 (겹치면 음수) — 글 본문에도 header가 있어 사이트 헤더(첫 번째)로 한정
      const header = page.locator("header").first();
      await expect
        .poll(async () => {
          const headingBox = await heading.boundingBox();
          const headerBox = await header.boundingBox();
          if (!headingBox || !headerBox) return -1;
          return headingBox.y - (headerBox.y + headerBox.height);
        })
        .toBeGreaterThanOrEqual(0);
    });
  }
});

test("/ken/{id}로 진입하면 /p/{id}로 리다이렉트된다", async ({ page }) => {
  const title = uniqueTitle("e2e-ken");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content: "구 URL 리다이렉트 검증용 본문",
    published: true,
    listed: false,
  });
  await api.dispose();

  await page.goto(`/ken/${id}`);

  await expect(page).toHaveURL(new RegExp(`/p/${id}$`));
  await expect(
    page.getByRole("heading", { level: 1, name: title }),
  ).toBeVisible();
});
