import { create } from "zustand";
import { BUDGET, type DateType, type Region } from "@/lib/course-options";

// 코스 생성 입력값 (지역 / 유형 / 예산 / 만 19세 자가 확인)
interface CourseInputState {
  region: Region | null;
  dateType: DateType | null;
  budget: number;
  // 비로그인 자가 확인: 체크 시 주류 장소 포함 (prd.md FR-1.2)
  isAdultConfirmed: boolean;
  setRegion: (region: Region) => void;
  setDateType: (dateType: DateType) => void;
  setBudget: (budget: number) => void;
  setAdultConfirmed: (confirmed: boolean) => void;
  reset: () => void;
}

const initialState = {
  region: null,
  dateType: null,
  budget: BUDGET.default,
  isAdultConfirmed: false,
};

export const useCourseInputStore = create<CourseInputState>((set) => ({
  ...initialState,
  setRegion: (region) => set({ region }),
  setDateType: (dateType) => set({ dateType }),
  setBudget: (budget) => set({ budget }),
  setAdultConfirmed: (isAdultConfirmed) => set({ isAdultConfirmed }),
  reset: () => set(initialState),
}));
