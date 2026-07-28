const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const activeConnections = new Map<string, EventSource>();

export interface SseSubscription {
  unsubscribe: () => void;
}

export function subscribe(
  channel: string,
  callback: (data: string) => void,
): SseSubscription {
  // ADR-0001: 인증은 쿠키로 — withCredentials면 로그인 사용자의 쿠키가 함께 전송된다
  const eventSource = new EventSource(
    `${NEXT_PUBLIC_API_BASE_URL}/sse/${channel}`,
    { withCredentials: true },
  );

  eventSource.onopen = () => {
    console.info(`SSE connected: /sse/${channel}`);
  };

  eventSource.onerror = () => {
    console.error(`SSE error: /sse/${channel}`);
  };

  eventSource.addEventListener("message", (event: MessageEvent) => {
    callback(event.data);
  });

  activeConnections.set(channel, eventSource);

  return {
    unsubscribe: () => {
      eventSource.close();
      activeConnections.delete(channel);
    },
  };
}
