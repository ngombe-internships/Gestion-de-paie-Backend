package com.hades.paie1.repository;

import com.hades.paie1.model.BulletinTemplate;
import com.hades.paie1.model.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinTemplateRepository  extends JpaRepository<BulletinTemplate, Long> {
        @Query("SELECT t FROM BulletinTemplate t LEFT JOIN FETCH t.elementsConfig WHERE t.entreprise = :entreprise AND t.isDefault = true")
        Optional<BulletinTemplate> findByEntrepriseAndIsDefaultTrueWithElements(@Param("entreprise") Entreprise entreprise);
        List<BulletinTemplate> findByEntreprise(Entreprise entreprise);

}
