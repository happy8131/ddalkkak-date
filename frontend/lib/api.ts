import { API_BASE_URL } from "./env";

// 백엔드 헬스체크 응답 타입
export interface HealthResponse {
  status: string;
}

// 백엔드 헬스체크 호출 (Spring Boot Actuator 기본 경로)
export async function fetchHealth(): Promise<HealthResponse> {
  const res = await fetch(`${API_BASE_URL}/actuator/health`, {
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`헬스체크 실패: ${res.status}`);
  }
  return res.json();
}
