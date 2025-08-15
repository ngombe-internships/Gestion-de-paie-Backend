package com.hades.maalipo.repository;

import com.hades.maalipo.model.TypeCongeConfig;
import com.hades.maalipo.enum1.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TypeCongeConfigRepository extends JpaRepository<TypeCongeConfig, Long> {

    List<TypeCongeConfig> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    Optional<TypeCongeConfig> findByEntrepriseIdAndTypeConge(Long entrepriseId, TypeConge typeConge);


    @Query("SELECT tcc FROM TypeCongeConfig tcc WHERE tcc.entreprise.id = :entrepriseId " +
            "AND tcc.typeConge = :typeConge AND tcc.actif = true")
    Optional<TypeCongeConfig> findActiveConfigByEntrepriseAndType(
            @Param("entrepriseId") Long entrepriseId,
            @Param("typeConge") TypeConge typeConge
    );

    @Query("SELECT tcc FROM TypeCongeConfig tcc WHERE tcc.entreprise.id = :entrepriseId " +
            "AND tcc.actif = true ORDER BY tcc.typeConge")
    List<TypeCongeConfig> findAllActiveConfigsByEntreprise(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT tc FROM TypeCongeConfig tc WHERE tc.entreprise.id = :entrepriseId ORDER BY tc.typeConge")
    List<TypeCongeConfig> findAllConfigsByEntreprise(@Param("entrepriseId") Long entrepriseId);

}