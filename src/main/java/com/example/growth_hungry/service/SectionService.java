package com.example.growth_hungry.service;

import com.example.growth_hungry.model.Section;
import java.util.List;

public interface SectionService {
    Section getById(Long sectionId);

    List<Section> getByCourseId(Long courseId);

    void updateContent(Long sectionId, String content);

    String getSectionContent(Long sectionId);
}
