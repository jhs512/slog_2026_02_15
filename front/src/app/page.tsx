"use client";

import Link from "next/link";

import { useEffect, useState } from "react";

import PostCard from "@/domain/post/components/PostCard";
import PostWriteButton from "@/domain/post/components/PostWriteButton";
import { useAuthContext } from "@/global/auth/hooks/useAuth";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import AdSenseUnit from "@/lib/business/components/AdSenseUnit";
import LoginButton from "@/lib/business/components/LoginButton";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

import { ArrowRight, FileText, Sparkles } from "lucide-react";

type PostDto = components["schemas"]["PostDto"];

const LATEST_POSTS_COUNT = 6;

export default function Page() {
  const { isLogin } = useAuthContext();

  const [posts, setPosts] = useState<PostDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    client
      .GET("/post/api/v1/posts", {
        params: {
          query: {
            page: 1,
            pageSize: LATEST_POSTS_COUNT,
            kw: "",
            sort: "CREATED_AT",
          },
        },
      })
      .then((res) => {
        if (cancelled) return;
        if (res.data) setPosts(res.data.content);
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="container mx-auto px-4 py-10">
      <Card className="text-center">
        <CardContent className="pt-10 pb-8 space-y-6">
          <div className="space-y-4">
            <div className="flex justify-center">
              <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center">
                <Sparkles className="w-8 h-8 text-primary" />
              </div>
            </div>
            <h1 className="text-3xl font-bold">환영합니다</h1>
            <p className="text-muted-foreground">
              개발 여정을 기록하고, 지식을 나누세요
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Button asChild size="lg">
              <Link href="/p">
                <FileText className="w-4 h-4" />글 목록 보기
              </Link>
            </Button>
            {isLogin ? (
              <PostWriteButton variant="outline" size="lg" />
            ) : (
              <LoginButton variant="outline" text="로그인" />
            )}
          </div>
        </CardContent>
      </Card>

      <section className="mt-12">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold">최신 글</h2>
          <Button variant="ghost" asChild>
            <Link href="/p">
              전체 보기
              <ArrowRight className="w-4 h-4" />
            </Link>
          </Button>
        </div>

        {loading ? (
          <div className="py-12 text-center text-muted-foreground">
            로딩중...
          </div>
        ) : posts.length === 0 ? (
          <div className="py-12 text-center text-muted-foreground">
            아직 글이 없습니다.
          </div>
        ) : (
          <ul className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {posts.map((post) => (
              <li key={post.id}>
                <PostCard post={post} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <AdSenseUnit
        slot={process.env.NEXT_PUBLIC_ADSENSE_SLOT_MAIN}
        className="mt-12"
      />
    </div>
  );
}
