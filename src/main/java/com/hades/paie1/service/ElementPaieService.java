package com.hades.paie1.service;

import com.hades.paie1.dto.ElementPaieDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.ElementPaie;
import com.hades.paie1.repository.ElementPaieRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ElementPaieService {
    private final ElementPaieRepository elementPaieRepository;

    public ElementPaieService(ElementPaieRepository elementPaieRepository) {
        this.elementPaieRepository = elementPaieRepository;
    }

    private ElementPaieDto convertToDto(ElementPaie elementPaie) {
        ElementPaieDto dto = new ElementPaieDto();
        dto.setId(elementPaie.getId());
        dto.setCode(elementPaie.getCode());

        dto.setType(elementPaie.getType());
        dto.setFormuleCalcul(elementPaie.getFormuleCalcul());
        dto.setTauxDefaut(elementPaie.getTauxDefaut());
        dto.setNombreDefaut(elementPaie.getNombreDefaut());
        dto.setMontantDefaut(elementPaie.getMontantDefaut());
//        dto.setUniteBaseCalcul(elementPaie.getUniteBaseCalcul());
        dto.setCategorie(elementPaie.getCategorie());
        dto.setDesignation(elementPaie.getDesignation());
        dto.setImpacteSalaireBrut(elementPaie.isImpacteSalaireBrut());
        dto.setImpacteBaseCnps(elementPaie.isImpacteBaseCnps());
        dto.setImpacteBaseIrpp(elementPaie.isImpacteBaseIrpp());
        dto.setImpacteSalaireBrutImposable(elementPaie.isImpacteSalaireBrutImposable());
        dto.setImpacteBaseCreditFoncier(elementPaie.isImpacteBaseCreditFoncier());
        dto.setImpacteBaseAnciennete(elementPaie.isImpacteBaseAnciennete());
        dto.setImpacteNetAPayer(elementPaie.isImpacteNetAPayer());
        return dto;
    }

    private ElementPaie convertToEntity(ElementPaieDto elementPaieDto) {
        ElementPaie elementPaie = new ElementPaie();
        // L'ID est souvent généré par la DB lors de la création, donc on ne le set pas ici pour une nouvelle entité
        // Si c'est pour une mise à jour, l'ID serait déjà présent dans l'objet récupéré de la DB
        elementPaie.setId(elementPaieDto.getId()); // Pour les mises à jour
        elementPaie.setCode(elementPaieDto.getCode());

        elementPaie.setType(elementPaieDto.getType());
        elementPaie.setFormuleCalcul(elementPaieDto.getFormuleCalcul());
        elementPaie.setTauxDefaut(elementPaieDto.getTauxDefaut());
        elementPaie.setMontantDefaut(elementPaieDto.getMontantDefaut());
        elementPaie.setNombreDefaut(elementPaieDto.getNombreDefaut());
        elementPaie.setCategorie(elementPaieDto.getCategorie());
        elementPaie.setDesignation(elementPaieDto.getDesignation());
        elementPaie.setImpacteSalaireBrut(elementPaieDto.isImpacteSalaireBrut());
        elementPaie.setImpacteBaseCnps(elementPaieDto.isImpacteBaseCnps());
        elementPaie.setImpacteBaseIrpp(elementPaieDto.isImpacteBaseIrpp());
        elementPaie.setImpacteSalaireBrutImposable(elementPaieDto.isImpacteSalaireBrutImposable());
        elementPaie.setImpacteBaseCreditFoncier(elementPaieDto.isImpacteBaseCreditFoncier());
        elementPaie.setImpacteBaseAnciennete(elementPaieDto.isImpacteBaseAnciennete());
        elementPaie.setImpacteNetAPayer(elementPaieDto.isImpacteNetAPayer());
        return elementPaie;
    }
    @Transactional
    // Ajout de @Transactional pour s'assurer que les entités sont gérées dans un contexte persistant avant conversion si nécessaire
    public List<ElementPaieDto> getAllElementsPaie() {
        return elementPaieRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<ElementPaieDto> getElementPaieById(Long id) {
        return elementPaieRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public ElementPaieDto createElementPaie(ElementPaie elementPaie) { // Accepte toujours l'entité de l'extérieur pour l'instant
        ElementPaie createdElement = elementPaieRepository.save(elementPaie);
        return convertToDto(createdElement); // Retourne le DTO
    }

    @Transactional
    public ElementPaieDto updateElementPaie(Long id, ElementPaie updatedElementPaie) { // Accepte toujours l'entité de l'extérieur pour l'instant
        ElementPaie existingElementPaie = elementPaieRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("ElementPaie non trouvé avec id :" + id));

        existingElementPaie.setCode(updatedElementPaie.getCode());
        existingElementPaie.setType(updatedElementPaie.getType());
        existingElementPaie.setFormuleCalcul(updatedElementPaie.getFormuleCalcul());
        existingElementPaie.setTauxDefaut(updatedElementPaie.getTauxDefaut());
        existingElementPaie.setMontantDefaut(updatedElementPaie.getMontantDefaut());
        existingElementPaie.setCategorie(updatedElementPaie.getCategorie());
        existingElementPaie.setNombreDefaut(updatedElementPaie.getNombreDefaut());
        existingElementPaie.setDesignation(updatedElementPaie.getDesignation());
        existingElementPaie.setImpacteSalaireBrut(updatedElementPaie.isImpacteSalaireBrut());
        existingElementPaie.setImpacteBaseCnps(updatedElementPaie.isImpacteBaseCnps());
        existingElementPaie.setImpacteBaseIrpp(updatedElementPaie.isImpacteBaseIrpp());
        existingElementPaie.setImpacteSalaireBrutImposable(updatedElementPaie.isImpacteSalaireBrutImposable());
        existingElementPaie.setImpacteBaseCreditFoncier(updatedElementPaie.isImpacteBaseCreditFoncier());
        existingElementPaie.setImpacteBaseAnciennete(updatedElementPaie.isImpacteBaseAnciennete());
        existingElementPaie.setImpacteNetAPayer(updatedElementPaie.isImpacteNetAPayer());

        ElementPaie savedElement = elementPaieRepository.save(existingElementPaie);
        return convertToDto(savedElement); // Retourne le DTO
    }

    public void deleteElementPaie(Long id) {
        if (!elementPaieRepository.existsById(id)) {
            throw new RessourceNotFoundException("ElementPaie non trouvé avec id :" + id);
        }
        elementPaieRepository.deleteById(id);
    }
}
