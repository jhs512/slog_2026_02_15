// e2e 프로필의 시드 계정 (백엔드 MemberNotProdInitData 기준) — 시드가 바뀌면 이 파일만 고친다
export const API_BASE = "http://localhost:8091";

export const ACCOUNTS = {
  user1: { username: "user1", password: "1234", nickname: "유저1" },
  user2: { username: "user2", password: "1234", nickname: "유저2" },
  admin: { username: "admin", password: "1234", nickname: "관리자" },
} as const;

export type AccountKey = keyof typeof ACCOUNTS;

export const storageStatePath = (key: AccountKey) => `e2e/.auth/${key}.json`;
