import { NextResponse } from "next/server";

// 애드센스는 도메인 루트의 /ads.txt 로 게시자를 확인한다.
// 퍼블리셔 ID를 환경변수로 두고 있어 정적 파일 대신 라우트로 생성한다.
const CLIENT_ID = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID;

// 구글 애드센스의 고정 인증기관 ID (모든 게시자가 동일하게 사용)
const GOOGLE_CERTIFICATION_AUTHORITY_ID = "f08c47fec0942fa0";

export function GET() {
  if (!CLIENT_ID) {
    return new NextResponse("Not Found", { status: 404 });
  }

  // ca-pub-0000000000000000 → pub-0000000000000000
  const publisherId = CLIENT_ID.replace(/^ca-/, "");

  return new NextResponse(
    `google.com, ${publisherId}, DIRECT, ${GOOGLE_CERTIFICATION_AUTHORITY_ID}\n`,
    {
      status: 200,
      headers: {
        "Content-Type": "text/plain; charset=utf-8",
        "Cache-Control": "public, max-age=3600",
      },
    },
  );
}
