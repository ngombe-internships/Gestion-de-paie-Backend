package com.hades.paie1.repository;

import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinTemplateRepository  extends JpaRepository<BulletinTemplate, Long> {
        Optional<BulletinTemplate> findByEntrepriseAndIsDefaultTrue(Entreprise entreprise);

        List<BulletinTemplate> findByEntreprise(Entreprise entreprise);

}
