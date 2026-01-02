package com.example.growth_hungry.repository;

import com.example.growth_hungry.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository  extends JpaRepository<Course, Long> {

}
