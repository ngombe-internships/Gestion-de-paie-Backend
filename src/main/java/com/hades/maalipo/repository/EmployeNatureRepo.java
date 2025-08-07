package com.hades.maalipo.repository;

import com.hades.maalipo.model.EmployeAvantageNature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeNatureRepo extends JpaRepository<EmployeAvantageNature, Long> {
}
