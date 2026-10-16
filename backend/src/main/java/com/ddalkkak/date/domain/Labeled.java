package com.ddalkkak.date.domain;

// API와 DB에서 쓰는 한글 표기("홍대", "문화데이트")를 가진 enum
public interface Labeled {

  String label();
}
