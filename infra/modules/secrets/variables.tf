variable "name_prefix" {
  description = "리소스 이름 접두사 (예: ddalkkak-date-prod). 파라미터 이름은 /<접두사>/<키> 가 된다"
  type        = string
}

variable "tags" {
  description = "공통 태그 (project, env, owner). 리소스에 추가로 붙일 때 쓴다"
  type        = map(string)
  default     = {}
}

variable "parameters" {
  description = "만들 SecureString 파라미터 (키: 이름, 값: 설명). 실제 값은 콘솔이나 CLI 로 주입한다"
  type        = map(string)
  default = {
    "kakao-rest-api-key"  = "카카오 Local API REST 키 (장소 수집 배치)"
    "llm-api-key"         = "LLM API 키 (OpenAI 호환, 기본 Groq. AI 큐레이션)"
    "anthropic-api-key"   = "Anthropic API 키 (AI_PROVIDER=anthropic 으로 Claude 를 쓸 때만)"
    "naver-client-id"     = "네이버 검색 API Client ID (블로그 후기 검색)"
    "naver-client-secret" = "네이버 검색 API Client Secret (블로그 후기 검색)"
  }
}
