import HealthStatus from "@/components/HealthStatus";

// 백엔드 연동 확인용 임시 페이지
export default function HealthPage() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-2xl font-bold">헬스체크</h1>
      <HealthStatus />
    </main>
  );
}
