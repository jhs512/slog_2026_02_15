// 개인정보처리방침 · 이용약관 공용 조각
//
// 주의: 아래 문서들은 실제 서비스 동작(소셜 로그인 항목, 인증 쿠키, GTM 사용 등)에
// 맞춰 작성한 일반적인 초안이다. 법률 검토를 받은 문서가 아니므로, 공개 전에
// LEGAL_INFO 의 운영자·연락처를 채우고 필요하면 전문가 검토를 거칠 것.

export const LEGAL_INFO = {
  siteName: "슬로그",
  siteUrl: "https://www.slog.gg",
  operator: "슬로그 운영팀",
  // TODO: 실제 문의용 이메일로 교체할 것
  contactEmail: "admin@slog.gg",
  effectiveDate: "2026년 8월 20일",
} as const;

export function LegalPage({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="container mx-auto px-4 py-10 max-w-3xl">
      <h1 className="text-3xl font-bold">{title}</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        시행일: {LEGAL_INFO.effectiveDate}
      </p>
      <div className="mt-10 space-y-10">{children}</div>
    </div>
  );
}

export function LegalSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">{title}</h2>
      <div className="space-y-3 break-keep text-sm leading-7 text-muted-foreground">
        {children}
      </div>
    </section>
  );
}

export function LegalList({ items }: { items: React.ReactNode[] }) {
  return (
    <ul className="list-disc space-y-1 pl-5">
      {items.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  );
}

export function LegalTable({
  headers,
  rows,
}: {
  headers: string[];
  rows: React.ReactNode[][];
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-border">
            {headers.map((header) => (
              <th
                key={header}
                className="py-2 pr-4 font-medium break-keep text-foreground"
              >
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex} className="border-b border-border/50 align-top">
              {row.map((cell, cellIndex) => (
                <td key={cellIndex} className="py-2 pr-4 leading-6 break-keep">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
