package com.example.growth_hungry.dto;

import java.time.Instant;
import java.util.List;

public class CourseDto {
    public Long id;
    public String title;
    public String description;
    public Instant createdAt;
    public Instant updatedAt;

    public List<SectionDto> sections;
}
