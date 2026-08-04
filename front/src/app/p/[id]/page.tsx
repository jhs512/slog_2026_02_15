import { cookies } from "next/headers";

import client from "@/global/backend/client";
import type { Metadata } from "next";

import {
  getSummaryFromContent,
  stripMarkdown,
} from "@/lib/business/markdownUtils";

import ClientAuthRetryPage from "./ClientAuthRetryPage";
import ClientPage from "./ClientPage";

async function getPost(id: number) {
  const res = await client.GET("/post/api/v1/posts/{id}", {
    params: {
      path: {
        id,
      },
    },
    headers: {
      cookie: (await cookies()).toString(),
    },
  });

  return res;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const postResponse = await getPost(parseInt(id));

  if (postResponse.error) {
    return {
      title: postResponse.error.msg,
      description: postResponse.error.msg,
    };
  }

  const post = postResponse.data;
  const summary = getSummaryFromContent(post.content);

  return {
    title: `${post.id} - ${post.title}`,
    description: summary || stripMarkdown(post.content),
  };
}

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const postResponse = await getPost(parseInt(id));

  if (postResponse.error) {
    // 401/403은 인증 쿠키가 Next 서버에 실리지 않아 생길 수 있으므로 브라우저에서 재조회한다.
    const resultCode = postResponse.error.resultCode ?? "";
    if (resultCode.startsWith("401") || resultCode.startsWith("403")) {
      return (
        <ClientAuthRetryPage
          postId={parseInt(id)}
          fallbackMsg={postResponse.error.msg}
        />
      );
    }

    return (
      <div className="flex-1 flex items-center justify-center">
        {postResponse.error.msg}
      </div>
    );
  }

  return <ClientPage initialPost={postResponse.data} />;
}
