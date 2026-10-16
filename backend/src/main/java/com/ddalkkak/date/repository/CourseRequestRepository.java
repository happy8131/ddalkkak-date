package com.ddalkkak.date.repository;

import com.ddalkkak.date.domain.CourseRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRequestRepository extends JpaRepository<CourseRequest, UUID> {}
