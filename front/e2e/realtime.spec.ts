import { expect, test } from "@playwright/test";

import { storageStatePath } from "./accounts";
import { apiAnon, apiAs, createPost, uniqueTitle } from "./api";

// 실시간 축: SSE 새 글 알림 토스트(조회수 10 돌파, 작성자 제외) + STOMP 글 수정 반영

test("조회수 10 돌파 시 구독자에게 토스트가 뜨고 작성자에게는 안 뜬다", async ({
  browser,
}) => {
  const title = uniqueTitle("e2e-rt-sse");

  const api = await apiAs("user1");
  // listed=false — 목록 페이지에 제목이 노출되지 않아 토스트의 제목만 매칭된다
  const id = await createPost(api, {
    title,
    content: "SSE 새 글 알림 검증용 본문",
    published: true,
    listed: false,
  });
  await api.dispose();

  const subscriberContext = await browser.newContext();
  const authorContext = await browser.newContext({
    storageState: storageStatePath("user1"),
  });

  try {
    const subscriberPage = await subscriberContext.newPage();
    const authorPage = await authorContext.newPage();

    // 두 브라우저 모두 SSE 연결이 열린 뒤에 조회수를 올린다
    const subscriberSse = subscriberPage.waitForResponse(
      (res) => res.url().includes("/sse/posts-new") && res.ok(),
    );
    await subscriberPage.goto("/p");
    await subscriberSse;

    const authorSse = authorPage.waitForResponse(
      (res) => res.url().includes("/sse/posts-new") && res.ok(),
    );
    await authorPage.goto("/p");
    await authorSse;

    // 익명 API로 조회수 증가 (Origin 헤더 없음)
    const anonApi = await apiAnon();
    for (let i = 1; i <= 9; i++) {
      const res = await anonApi.post(`/post/api/v1/posts/${id}/hit`);
      expect(res.ok(), `hit ${i}회차 실패: ${res.status()}`).toBeTruthy();
    }

    // 9회까지는 토스트 없음
    await expect(subscriberPage.getByText("새 글이 등록됨")).not.toBeVisible();

    // 10회째 — 돌파 순간 구독자에게 제목이 담긴 토스트가 뜬다
    const res10 = await anonApi.post(`/post/api/v1/posts/${id}/hit`);
    expect(res10.ok()).toBeTruthy();
    await anonApi.dispose();

    await expect(subscriberPage.getByText("새 글이 등록됨")).toBeVisible();
    await expect(
      subscriberPage.getByRole("link", { name: title }),
    ).toBeVisible();

    // 같은 이벤트가 구독자에게 도달했으므로, 작성자 브라우저에 안 뜬 것은 필터 동작이다
    await expect(authorPage.getByText("새 글이 등록됨")).not.toBeVisible();
  } finally {
    await subscriberContext.close();
    await authorContext.close();
  }
});

test("글 상세를 열어둔 브라우저에 다른 컨텍스트의 수정이 리로드 없이 반영된다", async ({
  page,
}) => {
  const title = uniqueTitle("e2e-rt-stomp");
  const contentBefore = `STOMP 수정 전 본문 ${title}`;
  const contentAfter = `STOMP 수정 후 본문 ${title}`;

  const api = await apiAs("user1");
  const id = await createPost(api, {
    title,
    content: contentBefore,
    published: true,
    listed: false,
  });

  // STOMP 구독(SUBSCRIBE 프레임 전송)이 끝난 뒤에 수정해야 메시지를 놓치지 않는다
  const stompSubscribed = new Promise<void>((resolve) => {
    page.on("websocket", (ws) => {
      ws.on("framesent", (frame) => {
        if (
          typeof frame.payload === "string" &&
          frame.payload.includes("SUBSCRIBE") &&
          frame.payload.includes(`/topic/posts/${id}/modified`)
        ) {
          resolve();
        }
      });
    });
  });

  await page.goto(`/p/${id}`);
  await expect(page.getByText(contentBefore)).toBeVisible();
  await stompSubscribed;

  const res = await api.put(`/post/api/v1/posts/${id}`, {
    data: {
      title,
      content: contentAfter,
      published: true,
      listed: false,
    },
  });
  expect(res.ok(), await res.text()).toBeTruthy();
  await api.dispose();

  // 리로드 없이 새 본문으로 갱신된다
  await expect(page.getByText(contentAfter)).toBeVisible();
  await expect(page.getByText(contentBefore)).not.toBeVisible();
});
