package com.hades.maalipo.repository;

import com.hades.maalipo.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByDemandeCongeId(Long demandeId);
}