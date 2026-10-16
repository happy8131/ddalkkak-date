"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import { useCourseInputStore } from "@/store/useCourseInputStore";
import { formatWon } from "@/lib/course-options";
import RegionMap from "./RegionMap";
import DateTypeCards from "./DateTypeCards";
import BudgetSlider from "./BudgetSlider";
import AdultCheck from "./AdultCheck";

function Section({
  step,
  title,
  children,
}: {
  step: number;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="flex items-center gap-2 text-lg font-bold">
        <span className="bg-secondary flex size-7 items-center justify-center rounded-full text-sm text-white">
          {step}
        </span>
        {title}
      </h2>
      {children}
    </section>
  );
}

export default function CourseInputForm() {
  const {
    region,
    dateType,
    budget,
    isAdultConfirmed,
    setRegion,
    setDateType,
    setBudget,
    setAdultConfirmed,
  } = useCourseInputStore();
  const [submitted, setSubmitted] = useState(false);

  const canSubmit = region !== null && dateType !== null;

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    // TODO: 코스 추천 API 연동은 후속 티켓에서 진행합니다.
    setSubmitted(true);
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex w-full flex-col gap-8 pb-28 sm:pb-8"
    >
      <Section step={1} title="어디서 만날까요?">
        <RegionMap value={region} onChange={setRegion} />
      </Section>

      <Section step={2} title="어떤 데이트를 원하세요?">
        <DateTypeCards value={dateType} onChange={setDateType} />
      </Section>

      <Section step={3} title="예산은 얼마로 할까요?">
        <BudgetSlider value={budget} onChange={setBudget} />
        <AdultCheck checked={isAdultConfirmed} onChange={setAdultConfirmed} />
      </Section>

      {/* 모바일에서는 하단 고정, 데스크톱에서는 폼 하단 */}
      <div className="bg-background/95 fixed inset-x-0 bottom-0 border-t border-black/5 p-4 backdrop-blur sm:static sm:border-0 sm:bg-transparent sm:p-0">
        <div className="mx-auto flex max-w-2xl flex-col gap-2">
          <p className="text-foreground/70 text-center text-sm" role="status">
            {submitted && canSubmit
              ? "코스 추천 기능은 곧 연결될 예정이에요!"
              : canSubmit
                ? `${region} · ${dateType} · ${formatWon(budget)}`
                : "지역과 데이트 유형을 선택해 주세요"}
          </p>
          <button
            type="submit"
            disabled={!canSubmit}
            className="bg-primary disabled:bg-foreground/20 w-full rounded-2xl py-4 text-lg font-bold text-white shadow-lg transition hover:brightness-95 active:scale-[0.98] disabled:cursor-not-allowed disabled:shadow-none"
          >
            추천 받기
          </button>
        </div>
      </div>
    </form>
  );
}
