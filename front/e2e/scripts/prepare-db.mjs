// slog_e2e DB가 없으면 만든다 (docker 컨테이너 db_1 전제 — back/devInfra)
import { execSync } from "node:child_process";

const DB_NAME = "slog_e2e";

function psql(sql) {
  return execSync(`docker exec db_1 psql -U postgres -tAc "${sql}"`, {
    encoding: "utf-8",
  }).trim();
}

try {
  const exists = psql(`SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'`);
  if (exists === "1") {
    console.log(`[e2e] DB ${DB_NAME} exists`);
  } else {
    psql(`CREATE DATABASE ${DB_NAME}`);
    console.log(`[e2e] DB ${DB_NAME} created`);
  }
} catch (e) {
  console.error(
    `[e2e] PostgreSQL 컨테이너(db_1)에 접근할 수 없습니다. back/devInfra에서 'docker compose up -d'를 먼저 실행하세요.`,
  );
  throw e;
}
