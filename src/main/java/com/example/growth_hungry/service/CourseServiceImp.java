package com.example.growth_hungry.service;

import com.example.growth_hungry.dto.CourseDto;
import com.example.growth_hungry.dto.SectionDto;
import com.example.growth_hungry.model.Course;
import com.example.growth_hungry.model.Section;
import com.example.growth_hungry.repository.CourseRepository;
import com.example.growth_hungry.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImp implements CourseService{

    private final CourseRepository courseRepository;

    public CourseServiceImp(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDto getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
        return toCourseDto(course);
    }
    private CourseDto toCourseDto(Course c) {
        CourseDto dto = new CourseDto();
        dto.id = c.getId();
        dto.title = c.getTitle();
        dto.description = c.getDescription();
        dto.createdAt = c.getCreatedAt();
        dto.updatedAt = c.getUpdatedAt();
        dto.sections = (c.getSections() == null) ? List.of()
                : c.getSections().stream().map(this::toSectionDto).toList();

        return dto;
    }

    private SectionDto toSectionDto(Section s) {
        SectionDto dto = new SectionDto();
        dto.id = s.getId();
        dto.content = s.getContent();
        dto.courseId = s.getCourse().getId();
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toCourseDto)
                .toList();
    }
}
