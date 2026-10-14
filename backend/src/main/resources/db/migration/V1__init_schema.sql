-- 초기 스키마 (prd.md "Database Schema (핵심 테이블)" 기준)

-- 사용자
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  kakao_id VARCHAR(100) UNIQUE NOT NULL,
  nickname VARCHAR(50),
  age_range VARCHAR(20),
  is_adult BOOLEAN,
  created_at TIMESTAMP DEFAULT NOW(),
  last_login_at TIMESTAMP
);

-- 장소
CREATE TABLE places (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  region VARCHAR(50) NOT NULL,
  category VARCHAR(50) NOT NULL,
  address TEXT NOT NULL,
  latitude DECIMAL(10, 8),
  longitude DECIMAL(11, 8),
  price_range INTEGER NOT NULL,
  estimated_time VARCHAR(20),
  kakao_place_id VARCHAR(50) UNIQUE,
  kakao_place_url TEXT,
  trust_score DECIMAL(3, 2),
  data_source VARCHAR(20) NOT NULL DEFAULT 'kakao',
  is_alcohol BOOLEAN DEFAULT FALSE,
  user_rating DECIMAL(2, 1),
  is_active BOOLEAN DEFAULT TRUE,
  last_updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_places_region_category ON places (region, category);
CREATE INDEX idx_places_active_rating ON places (is_active, user_rating DESC);

-- 장소 관련 블로그 후기 (제목/스니펫/링크만 저장, 본문 저장 금지)
CREATE TABLE place_reviews (
  id BIGSERIAL PRIMARY KEY,
  place_id BIGINT REFERENCES places (id),
  source VARCHAR(20) NOT NULL,
  title TEXT NOT NULL,
  snippet TEXT,
  link TEXT NOT NULL UNIQUE,
  blogger_name VARCHAR(100),
  posted_at DATE,
  match_confidence DECIMAL(3, 2),
  is_hidden BOOLEAN DEFAULT FALSE,
  fetched_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_place_reviews_place_posted ON place_reviews (place_id, posted_at DESC);

-- 코스 생성 요청
CREATE TABLE course_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id BIGINT REFERENCES users (id),
  guest_id VARCHAR(64),
  region VARCHAR(50) NOT NULL,
  date_type VARCHAR(50) NOT NULL,
  budget INTEGER NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  response_time_ms INTEGER
);

CREATE INDEX idx_course_requests_user_created ON course_requests (user_id, created_at DESC);

-- 생성된 코스
CREATE TABLE courses (
  id VARCHAR(50) PRIMARY KEY,
  request_id UUID REFERENCES course_requests (id),
  title VARCHAR(100) NOT NULL,
  total_cost INTEGER NOT NULL,
  total_time VARCHAR(20),
  places_json JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- 저장된 코스
CREATE TABLE saved_courses (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users (id),
  course_id VARCHAR(50) REFERENCES courses (id),
  saved_at TIMESTAMP DEFAULT NOW(),
  visited_at TIMESTAMP,
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_saved_courses_user_saved ON saved_courses (user_id, saved_at DESC);

-- 피드백
CREATE TABLE feedbacks (
  id BIGSERIAL PRIMARY KEY,
  saved_course_id BIGINT REFERENCES saved_courses (id),
  overall_rating INTEGER CHECK (overall_rating BETWEEN 1 AND 5),
  visited BOOLEAN NOT NULL,
  comment TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- 장소별 피드백
CREATE TABLE place_feedbacks (
  id BIGSERIAL PRIMARY KEY,
  feedback_id BIGINT REFERENCES feedbacks (id),
  place_id BIGINT REFERENCES places (id),
  still_open BOOLEAN NOT NULL,
  actual_cost INTEGER,
  rating INTEGER CHECK (rating BETWEEN 1 AND 5),
  visit_date DATE NOT NULL
);

CREATE INDEX idx_place_feedbacks_place_visit ON place_feedbacks (place_id, visit_date DESC);
