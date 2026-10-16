import Link from "next/link";

export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-primary text-3xl font-bold">데이트 딸깍</h1>
      <p>AI가 추천하는 데이트 코스</p>
      <Link
        href="/course/new"
        className="bg-primary rounded-2xl px-8 py-4 text-lg font-bold text-white shadow-lg"
      >
        코스 만들기
      </Link>
      <Link href="/health" className="text-secondary underline">
        백엔드 헬스체크
      </Link>
    </main>
  );
}
