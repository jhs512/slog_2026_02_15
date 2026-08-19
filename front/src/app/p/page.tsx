"use client";

import { useRouter, useSearchParams } from "next/navigation";

import { Suspense, useEffect, useState } from "react";

import Pagination from "@/components/Pagination";
import PostCard from "@/domain/post/components/PostCard";
import PostWriteButton from "@/domain/post/components/PostWriteButton";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import SearchHelpButton from "@/lib/business/components/SearchHelpButton";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import { ArrowUpDown, Search } from "lucide-react";

type PostDto = components["schemas"]["PostDto"];
type PageableDto = components["schemas"]["PageableDto"];
type PostSort =
  | "CREATED_AT"
  | "CREATED_AT_ASC"
  | "MODIFIED_AT"
  | "MODIFIED_AT_ASC";

const SORT_OPTIONS: { value: PostSort; label: string }[] = [
  { value: "CREATED_AT", label: "작성일 ↓" },
  { value: "CREATED_AT_ASC", label: "작성일 ↑" },
  { value: "MODIFIED_AT", label: "수정일 ↓" },
  { value: "MODIFIED_AT_ASC", label: "수정일 ↑" },
];

function PageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const currentPage = Math.max(1, Number(searchParams.get("page") || "1"));
  const currentPageSize = Math.min(
    100,
    Math.max(1, Number(searchParams.get("pageSize") || "30")),
  );
  const currentKw = searchParams.get("kw") || "";
  const currentSort = (searchParams.get("sort") || "CREATED_AT") as PostSort;

  const [posts, setPosts] = useState<PostDto[]>([]);
  const [pageable, setPageable] = useState<PageableDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [kwInput, setKwInput] = useState(currentKw);

  useEffect(() => {
    let cancelled = false;
    client
      .GET("/post/api/v1/posts", {
        params: {
          query: {
            page: currentPage,
            pageSize: currentPageSize,
            kw: currentKw,
            sort: currentSort,
          },
        },
      })
      .then((res) => {
        if (!cancelled && res.data) {
          setPosts(res.data.content);
          setPageable(res.data.pageable ?? null);
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [currentPage, currentPageSize, currentKw, currentSort]);

  const updateParams = (params: Record<string, string>) => {
    const sp = new URLSearchParams(searchParams.toString());
    for (const [k, v] of Object.entries(params)) {
      if (v) {
        sp.set(k, v);
      } else {
        sp.delete(k);
      }
    }
    router.push(`?${sp.toString()}`);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    updateParams({ kw: kwInput, page: "1" });
  };

  const handleSortChange = (sort: string) => {
    updateParams({ sort, page: "1" });
  };

  const handlePageChange = (page: number) => {
    updateParams({ page: String(page) });
  };

  if (loading)
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-muted-foreground">로딩중...</div>
      </div>
    );

  const totalPages = pageable?.totalPages ?? 1;
  const totalElements = pageable?.totalElements ?? posts.length;

  return (
    <div className="container mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-center my-4">공개글</h1>

      {/* 검색 */}
      <form onSubmit={handleSearch} className="flex gap-2 mb-4">
        <Input
          placeholder="검색어를 입력하세요"
          value={kwInput}
          onChange={(e) => setKwInput(e.target.value)}
          className="flex-1"
        />
        <Button type="submit" variant="outline" size="icon">
          <Search className="w-4 h-4" />
        </Button>
        <SearchHelpButton />
      </form>

      {/* 정렬 + 총 개수 */}
      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-muted-foreground">
          총 {totalElements}개
        </span>
        <div className="flex items-center gap-2">
          <ArrowUpDown className="w-4 h-4 text-muted-foreground" />
          <select
            value={currentSort}
            onChange={(e) => handleSortChange(e.target.value)}
            className="text-sm border rounded px-2 py-1 bg-background"
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <PostWriteButton />
        </div>
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-280px)] py-12 text-muted-foreground">
          <Search className="w-12 h-12 mb-4" />
          <p>글이 없습니다.</p>
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

      {/* 페이지네이션 */}
      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={handlePageChange}
        className="mt-8"
      />
    </div>
  );
}

export default function Page() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="text-muted-foreground">로딩중...</div>
        </div>
      }
    >
      <PageContent />
    </Suspense>
  );
}
