"use client";

import { REGIONS, type Region } from "@/lib/course-options";

interface RegionMapProps {
  value: Region | null;
  onChange: (region: Region) => void;
}

// 서울 5대 상권 일러스트 지도. 지도는 장식용 SVG이고 선택은 그 위의 버튼(radio)으로 합니다.
export default function RegionMap({ value, onChange }: RegionMapProps) {
  return (
    <div className="relative aspect-[4/3] w-full overflow-hidden rounded-3xl bg-[#FFF3DC] shadow-inner">
      <svg
        viewBox="0 0 100 75"
        preserveAspectRatio="none"
        className="absolute inset-0 h-full w-full"
        aria-hidden="true"
      >
        {/* 서울 윤곽 */}
        <path
          d="M8 30 C 10 14, 28 6, 46 8 C 62 4, 84 10, 92 24 C 98 36, 96 52, 88 62 C 76 72, 56 72, 40 70 C 24 70, 10 62, 6 50 C 4 42, 6 36, 8 30 Z"
          className="fill-white stroke-[#F4D9A6]"
          strokeWidth="0.8"
        />
        {/* 북한산 · 남산 */}
        <path d="M44 14 l4 -6 l4 6 Z" className="fill-[#C9E8C2]" />
        <path d="M50 15 l3 -4 l3 4 Z" className="fill-[#B5DFAE]" />
        <circle cx="46" cy="34" r="3.2" className="fill-[#C9E8C2]" />
        {/* 한강 */}
        <path
          d="M2 40 C 18 44, 26 50, 40 48 C 54 46, 60 52, 72 50 C 84 48, 90 44, 100 46"
          className="stroke-secondary/60 fill-none"
          strokeWidth="4"
          strokeLinecap="round"
        />
        <text
          x="86"
          y="53"
          className="fill-secondary text-[2.6px] font-semibold"
          textAnchor="middle"
        >
          한강
        </text>
      </svg>

      <div
        role="radiogroup"
        aria-label="지역 선택"
        className="absolute inset-0"
      >
        {REGIONS.map((region) => {
          const selected = value === region.name;
          return (
            <button
              key={region.id}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(region.name)}
              style={{ left: `${region.x}%`, top: `${region.y}%` }}
              className={`focus-visible:ring-primary/40 absolute flex min-h-11 min-w-16 -translate-x-1/2 -translate-y-1/2 flex-col items-center rounded-2xl border-2 px-3 py-1.5 text-sm font-bold shadow-md transition focus-visible:ring-4 focus-visible:outline-none active:scale-95 sm:min-w-20 sm:px-4 sm:py-2 sm:text-base ${
                selected
                  ? "border-primary bg-primary z-10 scale-110 text-white"
                  : "text-foreground hover:border-primary/50 border-white bg-white/95"
              }`}
            >
              <span>📍 {region.name}</span>
              <span
                className={`hidden text-[11px] font-medium sm:block ${
                  selected ? "text-white/90" : "text-foreground/60"
                }`}
              >
                {region.description}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
