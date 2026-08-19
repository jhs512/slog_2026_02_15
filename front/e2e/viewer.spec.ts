import { expect, test } from "@playwright/test";

import { apiAs, createPost, uniqueTitle } from "./api";

// 뷰어 렌더링 회귀: 코드펜스 이스케이프, <details> 접힘, raw 서빙

test("코드펜스 안의 HTML 문자열은 태그로 파싱되지 않는다", async ({ page }) => {
  const title = uniqueTitle("e2e-viewer-fence");
  // Prism에 없는 언어(text)·무언어 펜스는 하이라이팅을 안 타서
  // 예전에는 원문이 그대로 innerHTML에 꽂혔다 (<s>가 열려 문서 끝까지 취소선).
  const content = [
    "```text",
    "<s>취소선 유발",
    '<iframe src="https://evil.example/x"></iframe>',
    "```",
    "",
    "```",
    "<s>무언어 펜스",
    "```",
    "",
    "펜스 뒤 문단",
  ].join("\n");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content,
    published: true,
    listed: true,
  });
  await api.dispose();

  await page.goto(`/p/${id}`);

  const contents = page.locator(".toastui-editor-contents");
  await expect(contents.getByText("펜스 뒤 문단")).toBeVisible();

  // 원문이 태그가 아니라 텍스트로 남아야 한다
  await expect(contents.locator("s")).toHaveCount(0);
  await expect(contents.locator("iframe")).toHaveCount(0);
  await expect(contents.locator("pre code").first()).toContainText(
    "<s>취소선 유발",
  );
});

test("<details> 안의 마크다운이 토글 안에 들어간다", async ({ page }) => {
  const title = uniqueTitle("e2e-viewer-details");
  // <summary> 뒤 빈 줄에서 HTML 블록이 끝나는 CommonMark 규칙 때문에
  // 예전에는 코드펜스가 <details>의 형제로 빠져나가 항상 펼쳐진 채로 보였다.
  const content = [
    '<details raw-id="install">',
    "<summary>펼쳐보기</summary>",
    "",
    "안내 문단",
    "",
    "```sql",
    "SELECT 1;",
    "",
    "SELECT 2;",
    "```",
    "",
    "</details>",
    "",
    "토글 뒤 문단",
  ].join("\n");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content,
    published: true,
    listed: true,
  });
  await api.dispose();

  await page.goto(`/p/${id}`);

  const contents = page.locator(".toastui-editor-contents");
  const details = contents.locator("details");
  await expect(details).toHaveCount(1);

  // <summary>는 토글이 동작하려면 details의 직접 자식이어야 한다
  await expect(details.locator("> summary")).toHaveCount(1);

  // 본문·코드블록이 details 안에 있어야 한다
  const sql = details.locator("pre code");
  await expect(sql).toHaveCount(1);
  await expect(details.getByText("안내 문단")).toHaveCount(1);

  // 접힌 상태에서는 안 보이고, 펼치면 보인다 → 실제로 안에 들어있다는 증거
  await expect(sql).toBeHidden();
  await details.locator("> summary").click();
  await expect(sql).toBeVisible();

  // 코드 본문의 빈 줄과 하이라이팅이 살아있어야 한다
  await expect(sql).toContainText("SELECT 1;");
  await expect(sql).toContainText("SELECT 2;");
  await expect(sql.locator("span.token")).not.toHaveCount(0);

  // 토글 밖 문단은 그대로
  await expect(contents.getByText("토글 뒤 문단")).toBeVisible();
});

test("raw 서빙은 펜스와 <pre><code> 를 모두 인정한다", async ({ page }) => {
  const title = uniqueTitle("e2e-viewer-raw");
  const content = [
    '<details raw-id="fenced">',
    "<summary>펜스</summary>",
    "",
    "```sql",
    "SELECT 'fenced';",
    "```",
    "",
    "</details>",
    "",
    '<details raw-id="html">',
    "<summary>원시 HTML</summary>",
    '<pre><code data-language="sql">SELECT &#39;pre-code&#39; &amp;&amp; 1 &lt; 2;</code></pre>',
    "</details>",
  ].join("\n");

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content,
    published: true,
    listed: true,
  });
  await api.dispose();

  const fenced = await page.request.get(`/p/${id}/raw/fenced`);
  expect(fenced.status(), await fenced.text()).toBe(200);
  expect(await fenced.text()).toBe("SELECT 'fenced';");

  const html = await page.request.get(`/p/${id}/raw/html`);
  expect(html.status(), await html.text()).toBe(200);
  // HTML 엔티티는 원문으로 되돌아와야 한다
  expect(await html.text()).toBe("SELECT 'pre-code' && 1 < 2;");
});
