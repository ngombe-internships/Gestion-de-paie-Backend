package com.hades.paie1.repository;

import com.hades.paie1.model.LigneBulletinPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneBulletinPaieRepo extends JpaRepository<LigneBulletinPaie, Long> {
}
