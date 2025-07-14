package com.hades.paie1.repository;

import com.hades.paie1.model.EmployeAvantageNature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeNatureRepo extends JpaRepository<EmployeAvantageNature, Long> {
}
