"use client";

import { useEffect, useRef } from "react";

import { cn } from "@/lib/utils";

// 값이 없으면 광고 관련 요소를 아예 렌더하지 않는다 (스크립트도 layout에서 안 붙음)
const CLIENT_ID = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID;

export interface AdSenseUnitProps {
  /** 애드센스 대시보드에서 만든 광고 단위의 슬롯 ID */
  slot?: string;
  className?: string;
  /** 광고 형식. 기본 "auto" (반응형) */
  format?: string;
}

export default function AdSenseUnit({
  slot,
  className,
  format = "auto",
}: AdSenseUnitProps) {
  const isConfigured = Boolean(CLIENT_ID && slot);
  // 개발 중에 자기 광고를 노출·클릭하면 무효 트래픽이 되므로 운영에서만 실제로 띄운다
  const isLive = isConfigured && process.env.NODE_ENV === "production";

  // StrictMode의 이펙트 두 번 실행으로 같은 슬롯을 중복 push 하지 않도록 한다
  const pushedRef = useRef(false);

  useEffect(() => {
    if (!isLive || pushedRef.current) return;
    pushedRef.current = true;

    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const w = window as any;
      (w.adsbygoogle = w.adsbygoogle || []).push({});
    } catch {
      // 광고 차단기 등으로 스크립트가 없을 수 있다. 광고만 비고 페이지는 정상 동작해야 한다.
    }
  }, [isLive]);

  if (!isConfigured) return null;

  if (!isLive) {
    return (
      <div
        className={cn(
          "flex min-h-24 items-center justify-center rounded-md border border-dashed border-border text-xs text-muted-foreground",
          className,
        )}
      >
        광고 영역 (개발 모드에서는 노출되지 않음)
      </div>
    );
  }

  return (
    <ins
      className={cn("adsbygoogle block", className)}
      style={{ display: "block" }}
      data-ad-client={CLIENT_ID}
      data-ad-slot={slot}
      data-ad-format={format}
      data-full-width-responsive="true"
    />
  );
}
