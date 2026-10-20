-- 장소 수집 배치(KAN-16): AI 큐레이션 결과와 수작업 큐레이션 표시 컬럼
ALTER TABLE places
  ADD COLUMN date_score INTEGER CHECK (date_score BETWEEN 1 AND 10),
  ADD COLUMN mood_tags JSONB,
  -- AI가 추정한 1인당 가격대 표시용 문자열 (예: '30,000-50,000원'). 계산에는 price_range(정수)를 쓴다
  ADD COLUMN price_range_label VARCHAR(50),
  ADD COLUMN best_time VARCHAR(50),
  ADD COLUMN recommendation TEXT,
  ADD COLUMN curated_at TIMESTAMP,
  -- true이면 price_range/estimated_time/is_alcohol을 AI 큐레이션이 덮어쓰지 않는다
  ADD COLUMN is_manual_curated BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_places_region_date_score ON places (region, date_score DESC);
