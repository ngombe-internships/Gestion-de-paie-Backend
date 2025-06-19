package com.hades.paie1.repository;

import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepo extends JpaRepository <BulletinPaie, Long> {

    List<BulletinPaie> findByEmploye (Employe employe);
    List<BulletinPaie> findByEmployeId(Long employeId);

}
