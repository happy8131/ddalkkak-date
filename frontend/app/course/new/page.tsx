import type { Metadata } from "next";
import CourseInputForm from "@/components/course/CourseInputForm";

export const metadata: Metadata = {
  title: "코스 만들기 | 데이트 딸깍",
};

export default function NewCoursePage() {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 pt-8 sm:px-6 sm:pt-12">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold sm:text-3xl">
          <span className="text-primary">딸깍</span> 한 번이면 데이트 코스 완성
        </h1>
        <p className="text-foreground/70">
          지역, 데이트 유형, 예산만 골라 주세요.
        </p>
      </header>
      <CourseInputForm />
    </main>
  );
}
