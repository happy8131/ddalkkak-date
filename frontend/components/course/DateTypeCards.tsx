"use client";

import { DATE_TYPES, type DateType } from "@/lib/course-options";

interface DateTypeCardsProps {
  value: DateType | null;
  onChange: (dateType: DateType) => void;
}

export default function DateTypeCards({ value, onChange }: DateTypeCardsProps) {
  return (
    <div
      role="radiogroup"
      aria-label="데이트 유형 선택"
      className="grid grid-cols-2 gap-3 sm:grid-cols-4"
    >
      {DATE_TYPES.map((type) => {
        const selected = value === type.value;
        return (
          <button
            key={type.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(type.value)}
            className={`focus-visible:ring-primary/40 flex flex-col items-center gap-1 rounded-2xl border-2 p-4 text-center transition focus-visible:ring-4 focus-visible:outline-none active:scale-95 ${
              selected
                ? "border-primary bg-primary/10"
                : "hover:border-primary/40 border-transparent bg-white shadow-sm"
            }`}
          >
            <span className="text-3xl" aria-hidden="true">
              {type.emoji}
            </span>
            <span
              className={`font-bold ${selected ? "text-primary" : "text-foreground"}`}
            >
              {type.label}
            </span>
            <span className="text-foreground/60 text-xs">
              {type.description}
            </span>
          </button>
        );
      })}
    </div>
  );
}
