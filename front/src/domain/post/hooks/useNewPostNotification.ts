"use client";

import { useEffect, useRef, useState } from "react";

import { useAuthContext } from "@/global/auth/hooks/useAuth";
import { subscribe } from "@/global/sse/sseClient";

export interface PostNotification {
  id: number;
  title: string;
  authorId: number;
  authorName: string;
  authorProfileImgUrl: string;
  createdAt: string;
}

export function useNewPostNotification(
  onNewPost?: (post: PostNotification) => void,
) {
  const [latestPost, setLatestPost] = useState<PostNotification | null>(null);
  const { loginMember, isPending } = useAuthContext();
  const callbackRef = useRef(onNewPost);
  const loginMemberRef = useRef(loginMember);

  useEffect(() => {
    callbackRef.current = onNewPost;
  }, [onNewPost]);

  useEffect(() => {
    loginMemberRef.current = loginMember;
  }, [loginMember]);

  useEffect(() => {
    // 인증 확정 전에는 구독을 보류한다 (익명/인증 연결이 뒤섞이는 것 방지)
    if (isPending) return;

    const subscription = subscribe("posts-new", (data) => {
      const post: PostNotification = JSON.parse(data);

      // 작성자 본인이면 무시
      if (loginMemberRef.current?.id === post.authorId) return;

      setLatestPost(post);
      callbackRef.current?.(post);
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [isPending]);

  return { latestPost };
}
