package com.hades.maalipo.repository;

import com.hades.maalipo.model.ElementPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ElementPaieRepository extends JpaRepository<ElementPaie, Long> {

    Optional<ElementPaie> findByCode (String code);

    boolean existsByCode (String code);

    Optional<ElementPaie> findByDesignation (String designation);


    List<ElementPaie> findByDesignationIn(List<String> designations);
}
