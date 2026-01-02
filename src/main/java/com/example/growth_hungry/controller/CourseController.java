package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.CourseDto;
import com.example.growth_hungry.service.CourseService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }
    @GetMapping
    public ResponseEntity<List<CourseDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());

    }
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getCourse(@PathVariable Long id){
        return ResponseEntity.ok(courseService.getCourse(id));
    }
}
