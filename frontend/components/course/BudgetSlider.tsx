"use client";

import { BUDGET, formatWon } from "@/lib/course-options";

interface BudgetSliderProps {
  value: number;
  onChange: (budget: number) => void;
}

export default function BudgetSlider({ value, onChange }: BudgetSliderProps) {
  const percent = ((value - BUDGET.min) / (BUDGET.max - BUDGET.min)) * 100;

  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <p
        className="text-primary text-center text-3xl font-bold"
        aria-live="polite"
      >
        {formatWon(value)}
      </p>
      <input
        type="range"
        aria-label="예산"
        aria-valuetext={formatWon(value)}
        min={BUDGET.min}
        max={BUDGET.max}
        step={BUDGET.step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        style={{
          background: `linear-gradient(to right, var(--color-primary) ${percent}%, #f1e6d6 ${percent}%)`,
        }}
        className="budget-slider mt-5 h-2 w-full cursor-pointer appearance-none rounded-full"
      />
      <div className="text-foreground/60 mt-2 flex justify-between text-xs">
        <span>{formatWon(BUDGET.min)}</span>
        <span>{formatWon(BUDGET.max)}</span>
      </div>
    </div>
  );
}
