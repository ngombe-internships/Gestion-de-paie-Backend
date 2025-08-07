package com.hades.maalipo.repository;

import com.hades.maalipo.model.BulletinTemplate;
import com.hades.maalipo.model.ElementPaie;
import com.hades.maalipo.model.TemplateElementPaieConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateElementPaieConfigRepository extends JpaRepository<TemplateElementPaieConfig, Long> {
    List<TemplateElementPaieConfig> findByBulletinTemplateAndIsActiveTrue(BulletinTemplate bulletinTemplate);

    List<TemplateElementPaieConfig> findByBulletinTemplate(BulletinTemplate bulletinTemplate);

    List<TemplateElementPaieConfig> findByElementPaie (ElementPaie elementPaie);


}
