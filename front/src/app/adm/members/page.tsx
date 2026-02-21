"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

import { Suspense, useEffect, useState } from "react";

import withAdmin from "@/global/auth/hoc/withAdmin";
import type { components } from "@/global/backend/apiV1/schema";
import client from "@/global/backend/client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import Pagination from "@/components/Pagination";

import { ArrowUpDown, Search } from "lucide-react";

type MemberWithUsernameDto = components["schemas"]["MemberWithUsernameDto"];
type PageableDto = components["schemas"]["PageableDto"];
type MemberKwType = "USERNAME" | "NICKNAME" | "ALL";
type MemberSort =
  | "CREATED_AT"
  | "CREATED_AT_ASC"
  | "USERNAME"
  | "USERNAME_ASC"
  | "NICKNAME"
  | "NICKNAME_ASC";

const KW_TYPE_OPTIONS: { value: MemberKwType; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "USERNAME", label: "사용자명" },
  { value: "NICKNAME", label: "별명" },
];

const SORT_OPTIONS: { value: MemberSort; label: string }[] = [
  { value: "CREATED_AT", label: "최신순" },
  { value: "CREATED_AT_ASC", label: "오래된순" },
  { value: "USERNAME", label: "사용자명 역순" },
  { value: "USERNAME_ASC", label: "사용자명순" },
  { value: "NICKNAME", label: "별명 역순" },
  { value: "NICKNAME_ASC", label: "별명순" },
];

function PageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const currentPage = Number(searchParams.get("page") || "1");
  const currentPageSize = Number(searchParams.get("pageSize") || "30");
  const currentKwType = (searchParams.get("kwType") || "ALL") as MemberKwType;
  const currentKw = searchParams.get("kw") || "";
  const currentSort = (searchParams.get("sort") || "CREATED_AT") as MemberSort;

  const [members, setMembers] = useState<MemberWithUsernameDto[]>([]);
  const [pageable, setPageable] = useState<PageableDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [kwInput, setKwInput] = useState(currentKw);
  const [kwTypeInput, setKwTypeInput] = useState<MemberKwType>(currentKwType);

  useEffect(() => {
    let cancelled = false;
    client
      .GET("/member/api/v1/adm/members", {
        params: {
          query: {
            page: currentPage,
            pageSize: currentPageSize,
            kwType: currentKwType,
            kw: currentKw,
            sort: currentSort,
          },
        },
      })
      .then((res) => {
        if (!cancelled && res.data) {
          setMembers(res.data.content);
          setPageable(res.data.pageable ?? null);
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [currentPage, currentPageSize, currentKwType, currentKw, currentSort]);

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
    updateParams({ kwType: kwTypeInput, kw: kwInput, page: "1" });
  };

  const handleSortChange = (sort: string) => {
    updateParams({ sort, page: "1" });
  };

  const handlePageChange = (page: number) => {
    updateParams({ page: String(page) });
  };

  if (loading) return <div>로딩중...</div>;

  const totalPages = pageable?.totalPages ?? 1;
  const totalElements = pageable?.totalElements ?? members.length;

  return (
    <div className="container mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-center my-4">회원 목록</h1>

      {/* 검색 */}
      <form onSubmit={handleSearch} className="flex gap-2 mb-4">
        <select
          value={kwTypeInput}
          onChange={(e) => setKwTypeInput(e.target.value as MemberKwType)}
          className="text-sm border rounded px-2 py-1 bg-background"
        >
          {KW_TYPE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <Input
          placeholder="검색어를 입력하세요"
          value={kwInput}
          onChange={(e) => setKwInput(e.target.value)}
          className="flex-1"
        />
        <Button type="submit" variant="outline" size="icon">
          <Search className="w-4 h-4" />
        </Button>
      </form>

      {/* 정렬 + 총 개수 */}
      <div className="flex items-center justify-between mb-6">
        <span className="text-sm text-muted-foreground">
          총 {totalElements}명
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
        </div>
      </div>

      {members.length === 0 ? (
        <div className="py-12 text-center text-muted-foreground">
          회원이 없습니다.
        </div>
      ) : (
        <ul className="space-y-2">
          {members.map((member) => (
            <li key={member.id}>
              <Link
                href={`./members/${member.id}`}
                className="block p-3 border rounded hover:bg-accent/50 transition-colors"
              >
                {member.id} : {member.username} / {member.name}
              </Link>
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

export default withAdmin(function Page() {
  return (
    <Suspense fallback={<div>로딩중...</div>}>
      <PageContent />
    </Suspense>
  );
});
