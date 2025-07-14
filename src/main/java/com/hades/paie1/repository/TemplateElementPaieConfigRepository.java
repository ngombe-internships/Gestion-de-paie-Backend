package com.hades.paie1.repository;

import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.model.TemplateElementPaieConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateElementPaieConfigRepository extends JpaRepository<TemplateElementPaieConfig, Long> {
    List<TemplateElementPaieConfig> findByBulletinTemplateAndIsActiveTrue(BulletinTemplate bulletinTemplate);

    List<TemplateElementPaieConfig> findByBulletinTemplate(BulletinTemplate bulletinTemplate);

    List<TemplateElementPaieConfig> findByElementPaie (ElementPaie elementPaie);


}
