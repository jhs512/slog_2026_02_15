"use client";

import { useEffect, useState } from "react";

import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import ClientPage from "./ClientPage";

type PostWithContentDto = components["schemas"]["PostWithContentDto"];

// 인증 쿠키는 Domain=api.slog.gg라 Next 서버(www)에는 실리지 않는다. 그래서 비공개 글은
// SSR 조회가 항상 403이 되고, 쿠키가 실리는 브라우저에서 재조회해야 작성자/관리자 여부를 알 수 있다.
export default function ClientAuthRetryPage({
  postId,
  fallbackMsg,
}: {
  postId: number;
  fallbackMsg: string;
}) {
  const [post, setPost] = useState<PostWithContentDto | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    client
      .GET("/post/api/v1/posts/{id}", { params: { path: { id: postId } } })
      .then((res) => {
        if (res.data) setPost(res.data);
        else setErrorMsg(res.error?.msg ?? fallbackMsg);
      })
      .catch(() => setErrorMsg(fallbackMsg));
  }, [postId, fallbackMsg]);

  if (post) return <ClientPage initialPost={post} />;

  return (
    <div className="flex-1 flex items-center justify-center">
      {errorMsg ?? "글을 불러오는 중..."}
    </div>
  );
}
