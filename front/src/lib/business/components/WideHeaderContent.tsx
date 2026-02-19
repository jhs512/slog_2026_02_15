"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useAuthContext } from "@/global/auth/hooks/useAuth";

import { Button } from "@/components/ui/button";

import { NotebookText, TableOfContents } from "lucide-react";

import LoginButton from "./LoginButton";
import Logo from "./Logo";
import MeMenuButton from "./MeMenuButton";
import PostWriteButton from "./PostWriteButton";
import ThemeToggleButton from "./ThemeToggleButton";

export default function WideHeaderContent({
  className,
}: {
  className?: string;
}) {
  const { isLogin } = useAuthContext();
  const pathname = usePathname();

  // /p/[id]/edit 또는 /p/[id]/edit/monaco 패턴에서 글 번호 추출
  const editMatch = pathname.match(/^\/p\/(\d+)\/edit/);
  const editPostId = editMatch ? editMatch[1] : null;

  return (
    <div className={`${className} container mx-auto px-4 py-1`}>
      <Button variant="link" asChild>
        <Logo text />
      </Button>
      <Button variant="link" asChild>
        <Link href="/p">
          <TableOfContents /> 글
        </Link>
      </Button>
      {isLogin && <PostWriteButton text />}
      {isLogin && (
        <Button variant="link" asChild>
          <Link href="/p/mine">
            <NotebookText /> 내글
          </Link>
        </Button>
      )}

      <div className="flex-grow flex items-center justify-center">
        {editPostId && (
          <Link
            href={`/p/${editPostId}/edit`}
            className="text-sm text-muted-foreground hover:underline"
          >
            #{editPostId}번 글
          </Link>
        )}
      </div>

      {!isLogin && <LoginButton />}
      {isLogin && <MeMenuButton />}
      <ThemeToggleButton />
    </div>
  );
}
