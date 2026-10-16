"use client";

interface AdultCheckProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
}

// 비로그인 사용자용 만 19세 자가 확인 (prd.md FR-1.2)
// 체크하지 않으면 주류 중심 장소(술집/바)는 추천 후보에서 제외됩니다.
export default function AdultCheck({ checked, onChange }: AdultCheckProps) {
  return (
    <label className="flex cursor-pointer items-start gap-3 rounded-2xl bg-white p-4 shadow-sm">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="accent-primary mt-0.5 size-5 shrink-0 cursor-pointer"
      />
      <span className="flex flex-col gap-0.5">
        <span className="font-semibold">만 19세 이상입니다</span>
        <span className="text-foreground/60 text-xs">
          체크하면 와인바 · 술집 같은 주류 장소도 코스에 포함돼요. 체크하지
          않으면 주류 장소는 제외됩니다.
        </span>
      </span>
    </label>
  );
}
