package com.example.growth_hungry.service;

import com.example.growth_hungry.model.Section;
import com.example.growth_hungry.repository.SectionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;

    public SectionServiceImpl(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Section getById(Long sectionId) {
        if (sectionId == null)
            throw new IllegalArgumentException("Section id is required");

        return sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Section> getByCourseId(Long courseId) {
        if (courseId == null)
            throw new IllegalArgumentException("Course id is required");

        return sectionRepository.findByCourseId(courseId);
    }

    @Override
    public void updateContent(Long sectionId, String content) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Content is required");

        Section section = getById(sectionId);
        section.setContent(content);
        // dirty checking сделает UPDATE
    }

    @Override
    @Transactional(readOnly = true)
    public String getSectionContent(Long sectionId) {
        String content = getById(sectionId).getContent();
        return content == null ? "" : content;
    }
}
