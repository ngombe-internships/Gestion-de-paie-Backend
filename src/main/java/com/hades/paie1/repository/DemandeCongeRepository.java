package com.hades.paie1.repository;

import com.hades.paie1.enum1.StatutDemandeConge;
import com.hades.paie1.model.DemandeConge;
import com.hades.paie1.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long > {

    List<DemandeConge> findByEmploye(Employe employe);
    List<DemandeConge> findByStatut(StatutDemandeConge statut);
    List<DemandeConge> findByEmployeAndStatut(Employe employe, StatutDemandeConge statut);


    List<DemandeConge> findByStatutOrderByDateDemandeAsc(StatutDemandeConge statut);

}

