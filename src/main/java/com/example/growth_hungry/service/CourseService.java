package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.CourseDto;
import java.util.List;

public interface CourseService {
    CourseDto getCourse(Long id);
    List<CourseDto> getAllCourses();
}
