import Link from "next/link";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-primary text-3xl font-bold">데이트 딸깍</h1>
      <p>AI가 추천하는 데이트 코스</p>
      <Link href="/health" className="text-secondary underline">
        백엔드 헬스체크
      </Link>
    </main>
  );
}
