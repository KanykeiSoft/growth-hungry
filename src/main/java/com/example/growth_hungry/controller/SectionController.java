package com.example.growth_hungry.controller;

import com.example.growth_hungry.dto.SectionDto;
import com.example.growth_hungry.model.Section;
import com.example.growth_hungry.service.SectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sections")
public class SectionController {
    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/{id}")
    public SectionDto getSection(@PathVariable Long id) {
        Section s = sectionService.getById(id);

        SectionDto dto = new SectionDto();
        dto.setId(s.getId());
        dto.setContent(s.getContent());
        dto.setCourseId(
                s.getCourse() == null ? null : s.getCourse().getId()
        );

        return dto;
    }

    @PutMapping("/{id}/content")
    public ResponseEntity<?> updateContent(@PathVariable Long id,
                                           @RequestBody SectionDto body) {
        // body.content содержит markdown
        sectionService.updateContent(id, body.getContent());
        return ResponseEntity.ok().build();
    }

}
