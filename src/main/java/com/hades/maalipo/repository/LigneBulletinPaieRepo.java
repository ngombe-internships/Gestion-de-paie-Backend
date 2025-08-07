package com.hades.maalipo.repository;

import com.hades.maalipo.model.LigneBulletinPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneBulletinPaieRepo extends JpaRepository<LigneBulletinPaie, Long> {
}
