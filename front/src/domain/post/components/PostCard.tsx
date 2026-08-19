"use client";

import Image from "next/image";
import Link from "next/link";

import type { components } from "@/global/backend/apiV1/schema";

import { formatDate } from "@/lib/utils";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { Eye, Heart, ListX, Lock, MessageCircle } from "lucide-react";

type PostDto = components["schemas"]["PostDto"];

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "";

function getImageUrl(path: string | undefined): string {
  if (!path) return "";
  if (path.startsWith("http")) return path;
  return `${API_BASE_URL}${path}`;
}

export default function PostCard({ post }: { post: PostDto }) {
  return (
    <Link href={`/p/${post.id}`}>
      <Card className="hover:bg-accent/50 transition-colors">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 break-all text-base">
            <Badge variant="outline">{post.id}</Badge>
            <span className="flex-1">{post.title}</span>
            {!post.published && (
              <span title={post.title ? "비공개" : "임시저장"}>
                <Lock className="w-4 h-4 flex-shrink-0 text-muted-foreground" />
              </span>
            )}
            {post.published && !post.listed && (
              <span title="미노출">
                <ListX className="w-4 h-4 flex-shrink-0 text-muted-foreground" />
              </span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-3">
            <Image
              src={
                getImageUrl(post.authorProfileImgUrl) || "/default-avatar.png"
              }
              alt={post.authorName}
              width={40}
              height={40}
              className="w-10 h-10 rounded-full object-cover ring-2 ring-primary/10"
              unoptimized
            />
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium">{post.authorName}</div>
              <div className="text-sm text-muted-foreground">
                {formatDate(post.createdAt)}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-4 mt-3 pt-3 border-t text-sm text-muted-foreground">
            <div className="flex items-center gap-1">
              <Eye className="w-4 h-4" />
              <span>{post.hitCount}</span>
            </div>
            <div className="flex items-center gap-1">
              <Heart
                className={`w-4 h-4 ${post.actorHasLiked ? "fill-red-500 text-red-500" : ""}`}
              />
              <span>{post.likesCount}</span>
            </div>
            <div className="flex items-center gap-1">
              <MessageCircle className="w-4 h-4" />
              <span>{post.commentsCount}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
