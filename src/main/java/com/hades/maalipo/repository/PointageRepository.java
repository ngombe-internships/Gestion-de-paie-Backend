//package com.hades.paie1.repository;
//
//import com.hades.paie1.model.Employe;
//import com.hades.paie1.model.Pointage;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.time.LocalDate;
//import java.util.List;
//
//public interface PointageRepository extends JpaRepository<Pointage, Long> {
//
//    List<Pointage> findByEmploye(Employe employe);
//
//    List<Pointage> findByEmployeAndDatePointageBetweenOrderByDatePointageAsc(
//            Employe employe, LocalDate startDate, LocalDate endDate);
//}
