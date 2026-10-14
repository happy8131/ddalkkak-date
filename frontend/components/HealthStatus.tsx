"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchHealth } from "@/lib/api";

export default function HealthStatus() {
  const { data, error, isLoading } = useQuery({
    queryKey: ["health"],
    queryFn: fetchHealth,
  });

  if (isLoading) return <p>백엔드 확인 중...</p>;
  if (error) return <p className="text-primary">연결 실패: {error.message}</p>;
  return <p className="text-secondary">백엔드 상태: {data?.status}</p>;
}
