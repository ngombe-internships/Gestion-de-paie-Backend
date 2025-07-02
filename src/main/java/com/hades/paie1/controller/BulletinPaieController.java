package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.dto.BulletinPaieEmployeurDto;
import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.service.BulletinPaieService;
import com.hades.paie1.service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bulletins")

public class BulletinPaieController {

    private BulletinPaieService bulletinPaieService;
    private PdfService pdfService;
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public BulletinPaieController(BulletinPaieService bulletinPaieService, PdfService pdf) {
        this.bulletinPaieService = bulletinPaieService;
        this.pdfService = pdf;
    }


    //cree bulletin avec  info de employe
    @PostMapping("/calculate1")

    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> calculerBulletin1(@RequestBody BulletinPaie fiche) {

        BulletinPaie calculBulletin = bulletinPaieService.calculBulletin(fiche);

        BulletinPaieResponseDto responseDto = bulletinPaieService.convertToDto(calculBulletin);
        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(

                "Bulletin de paie calcule avec succes",
                responseDto,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }


    // cree un bulletin et le sauvegarde
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> createBulletin(@RequestBody BulletinPaie fiche) {
        BulletinPaieResponseDto bulletinCalcule = bulletinPaieService.saveBulletinPaie(fiche);
        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                "Bulletin de paie calcule  et sauvegarde avec succes",
                bulletinCalcule,
                HttpStatus.CREATED
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDto>>> getAllBulletins() {

        List<BulletinPaieResponseDto> bulletins = bulletinPaieService.getAllBulletinsPaie();
        ApiResponse<List<BulletinPaieResponseDto>> response = new ApiResponse<>(
                "Liste de tous les bulletins de paie",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDto>>> getBulletinsForUserRole() {
        List<BulletinPaieResponseDto> bulletins = bulletinPaieService.getBulletinsFotCurrentUser();
        ApiResponse<List<BulletinPaieResponseDto>> response = new ApiResponse<>(
                "Liste des les bulletins de paie",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or  hasRole('EMPLOYEUR') or(hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#id))")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> getBulletinById(@PathVariable Long id) {

        Optional<BulletinPaieResponseDto> bulletinOptional = bulletinPaieService.getBulletinPaieById(id);

        if (bulletinOptional.isPresent()) {
            ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                    "Bulletin de paie trouvé",
                    bulletinOptional.get(),
                    HttpStatus.OK
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponse<BulletinPaieResponseDto> errorResponse = new ApiResponse<>(
                    "Bulletin de paie non trouvé avec ID: " + id,
                    null,
                    HttpStatus.NOT_FOUND
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> updateBulletin(@PathVariable Long id, @RequestBody BulletinPaie fiche) {
        BulletinPaieResponseDto bulletinUpdate = bulletinPaieService.updateBulletinPaie(id, fiche);

        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                "Bulletin de paie mis a jour avec succes",
                bulletinUpdate,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBulletin(@PathVariable Long id) {
        bulletinPaieService.deleteBulletinPaie(id);
        ApiResponse<Void> response = new ApiResponse<>(
                "Bulletin de paie supprime avec succes",
                null,
                HttpStatus.NO_CONTENT

        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }


    @GetMapping("/employeur")
    @PreAuthorize("hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<List<BulletinPaieEmployeurDto>>> getBulletinsForEmployeur(
            @RequestParam (required = false) String searchTerm) {
        List<BulletinPaieEmployeurDto> bulletins;

        if(searchTerm != null && !searchTerm.trim().isEmpty()){
            bulletins = bulletinPaieService.searchBulletinsForEmployer(searchTerm);
        } else {
            bulletins = bulletinPaieService.getBulletinsForEmployer();
        }
        ApiResponse<List<BulletinPaieEmployeurDto>> response = new ApiResponse<>(
                "Liste des bulletins de paie de votre entreprise",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/my-bulletins")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDto>>> getMyBulletins() {
        List<BulletinPaieResponseDto> bulletins = bulletinPaieService.getMyBulletins();
        ApiResponse<List<BulletinPaieResponseDto>> response = new ApiResponse<>(
                "Vos bulletins de paie",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Gestion des statuts
    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> validerBulletin(@PathVariable Long id) {
        try {
            BulletinPaieResponseDto updatedBulletin = bulletinPaieService.validerBulletin(id);
            return new ResponseEntity<>(new ApiResponse<>("Bulletin validé avec succès", updatedBulletin, HttpStatus.OK), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            logger.warn("Accès refusé pour valider le bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Non autorisé à valider ce bulletin.", null, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            logger.warn("Transition de statut invalide pour le bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage(), null, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la validation du bulletin {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(new ApiResponse<>("Erreur lors de la validation du bulletin.", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> envoyerBulletin(@PathVariable Long id) {
        try {
            BulletinPaieResponseDto updatedBulletin = bulletinPaieService.envoyerBulletin(id);
            return new ResponseEntity<>(new ApiResponse<>("Bulletin envoyé avec succès", updatedBulletin, HttpStatus.OK), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            logger.warn("Accès refusé pour envoyer le bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Non autorisé à envoyer ce bulletin.", null, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            logger.warn("Transition de statut invalide pour l'envoi du bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage(), null, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de l'envoi du bulletin {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(new ApiResponse<>("Erreur lors de l'envoi du bulletin.", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> archiverBulletin(@PathVariable Long id) {
        try {
            BulletinPaieResponseDto updatedBulletin = bulletinPaieService.archiverBulletin(id);
            return new ResponseEntity<>(new ApiResponse<>("Bulletin archivé avec succès", updatedBulletin, HttpStatus.OK), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            logger.warn("Accès refusé pour archiver le bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Non autorisé à archiver ce bulletin.", null, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            logger.warn("Transition de statut invalide pour l'archivage du bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage(), null, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de l'archivage du bulletin {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(new ApiResponse<>("Erreur lors de l'archivage du bulletin.", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> annulerBulletin(@PathVariable Long id) {
        try {
            BulletinPaieResponseDto updatedBulletin = bulletinPaieService.annulerBulletin(id);
            return new ResponseEntity<>(new ApiResponse<>("Bulletin annulé avec succès", updatedBulletin, HttpStatus.OK), HttpStatus.OK);
        } catch (AccessDeniedException e) {
            logger.warn("Accès refusé pour annuler le bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>("Non autorisé à annuler ce bulletin.", null, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
        } catch (IllegalStateException e) {
            logger.warn("Transition de statut invalide pour l'annulation du bulletin {}: {}", id, e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>(e.getMessage(), null, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            logger.error("Erreur lors de l'annulation du bulletin {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(new ApiResponse<>("Erreur lors de l'annulation du bulletin.", null, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/pdf/{bulletinId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEUR') or (hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#bulletinId)))")
    public ResponseEntity<byte[]> generatePdfEmploye(@PathVariable Long bulletinId) {

        try {
            Optional<BulletinPaieResponseDto> bulletinOptional = bulletinPaieService.getBulletinPaieById(bulletinId);

            if (bulletinOptional.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            BulletinPaieResponseDto bulletinCalcul = bulletinOptional.get();

            byte[] pdfBytes = pdfService.generateBulletinPdf(bulletinCalcul);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "bulletin_paie_" + bulletinCalcul.getEmploye().getNom()
                    + "_" + bulletinCalcul.getEmploye().getPrenom() + "_" + bulletinCalcul.getEmploye().getMatricule() + ".pdf";

            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {  // SEULE CETTE EXCEPTION EST MAINTENANT NÉCESSAIRE
            logger.error("Erreur lors de la génération du PDF pour le bulletin {}: {}", bulletinId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    @GetMapping(value = "/{id}/previews", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEUR') or (hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#id)))")
    public ResponseEntity<String> getBulletinHtml (@PathVariable Long id){
        try {
            BulletinPaieResponseDto bulletinData = bulletinPaieService.getBulletinById(id);

            String html = pdfService.generateHtmlContentForPdf(bulletinData);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la generation du html:" + e.getMessage());
        }
    }



    @GetMapping("/count")
    @PreAuthorize("hasRole('EMPLOYEUR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getBulletinCountForEmployer(){
        long count = bulletinPaieService.countBulletinsForAuthenticatedEmployer();

        ApiResponse<Long> response = new ApiResponse<>(
                "Total des bulletins recuperer avec succes",
                count,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }












    //calculer le formulaire sans employe
    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> calculerBulletin ( @RequestBody BulletinPaie fiche){

        BulletinPaieResponseDto bulletinCalcule = calculBulletin(fiche);

        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(

                "Bullerin de paie calcule avec succes" ,
                bulletinCalcule,
                HttpStatus.OK
        );
        return  new ResponseEntity<>(response, HttpStatus.OK);

    }


















    private BulletinPaieResponseDto calculBulletin (BulletinPaie fiche) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();

        //info de base
//        dto.setFicheOriginal(fiche);

        //calculs de salaire
        dto.setSalaireBase(bulletinPaieService.calculSalaireBase(fiche));
        dto.setTauxHoraire(fiche.getTauxHoraire());
        dto.setHeuresNormal(fiche.getHeuresNormal());
        dto.setPrimeTransport(fiche.getPrimeTransport());
        dto.setPrimePonctualite(fiche.getPrimePonctualite());
        dto.setPrimeTechnicite(fiche.getPrimeTechnicite());
        dto.setPrimeRendement(fiche.getPrimeRendement());
        dto.setPrimeAnciennete(fiche.getPrimeAnciennete());
        dto.setHeureSup1(bulletinPaieService.calculHeureSup1(fiche));
        dto.setHeureSup2(bulletinPaieService.calculHeureSup2(fiche));
        dto.setHeureNuit(bulletinPaieService.calculHeureNuit(fiche));
        dto.setHeureFerie(bulletinPaieService.calculHeureFerie(fiche));
        dto.setTotalPrimes(bulletinPaieService.calculTotalPrimes(fiche));
        dto.setSalaireBrut(bulletinPaieService.calculSalaireBrut(fiche));
        dto.setSalaireImposable(bulletinPaieService.calculSalaireImposable(fiche));
        dto.setBaseCnps(bulletinPaieService.calculBaseCnps(fiche));

        // Calculs d'impôts et taxes
        dto.setIrpp(bulletinPaieService.calculIrpp(fiche));
        dto.setCac(bulletinPaieService.calculCac(fiche));
        dto.setTaxeCommunale(bulletinPaieService.calculTaxeCommunale(fiche));
        dto.setRedevanceAudioVisuelle(bulletinPaieService.calculRedevanceAudioVisuelle(fiche));


        //calculs des cotisation salariales
        dto.setCnpsVieillesseSalarie(bulletinPaieService.calculCnpsVieillesseSalaire(fiche));
        dto.setCreditFoncierSalarie(bulletinPaieService.calculCreditFoncierSalaire(fiche));
        dto.setFneSalarie(bulletinPaieService.calculFneSalaire(fiche));
        dto.setTotalRetenues(bulletinPaieService.calculTotalRetenues(fiche));


        // Calculs des charges patronales
        dto.setCnpsVieillesseEmployeur(bulletinPaieService.calculCnpsVieillesseEmployeur(fiche));
        dto.setCnpsAllocationsFamiliales(bulletinPaieService.calculCnpsAllocationsFamiliales(fiche));
        dto.setCnpsAccidentsTravail(bulletinPaieService.calculCnpsAccidentsTravail(fiche));
        dto.setCreditFoncierPatronal(bulletinPaieService.calculCreditFoncierPatronal(fiche));
        dto.setFnePatronal(bulletinPaieService.calculFnePatronal(fiche));
        dto.setTotalChargesPatronales(bulletinPaieService.calculTotalChargesPatronales(fiche));

        // Calculs finaux
        dto.setSalaireNet(bulletinPaieService.calculSalaireNet(fiche));
        dto.setCoutTotalEmployeur(bulletinPaieService.calculCoutTotalEmployeur(fiche));

        //calcul Cotisation cnps
        dto.setCotisationCnps(bulletinPaieService.calculCotisationCnps(fiche));



        return dto;
    }



}
