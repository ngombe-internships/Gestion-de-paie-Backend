package com.hades.maalipo.controller;

import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.bulletin.BulletinPaieCreateDto;
import com.hades.maalipo.dto.bulletin.BulletinPaieEmployeurDto;
import com.hades.maalipo.dto.bulletin.BulletinPaieResponseDto;
import com.hades.maalipo.enum1.StatusBulletin;
import com.hades.maalipo.model.BulletinPaie;
import com.hades.maalipo.repository.BulletinPaieRepo;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.EntrepriseRepository;
import com.hades.maalipo.service.BulletinPaieService;
import com.hades.maalipo.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bulletins")

public class BulletinPaieController {

    private  final BulletinPaieService bulletinPaieService;
    private  final PdfService pdfService;
    private final  EmployeRepository employeRepository;
    private  final EntrepriseRepository entrepriseRepository;

    private final  BulletinPaieRepo bulletinPaieRepo;
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public BulletinPaieController(BulletinPaieService bulletinPaieService,
                                  PdfService pdf,
                                  EmployeRepository employeRepository,
                                  EntrepriseRepository entrepriseRepository,
                                  BulletinPaieRepo bulletinPaieRepo
    ) {
        this.bulletinPaieService = bulletinPaieService;
        this.pdfService = pdf;
        this.employeRepository = employeRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.bulletinPaieRepo = bulletinPaieRepo;
    }


    //cree bulletin avec  info de employe
    @PostMapping(value = "/calculate1")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> calculerBulletin1(
            @RequestBody BulletinPaieCreateDto ficheDto
    ) {
        BulletinPaieResponseDto responseDto = bulletinPaieService.calculateBulletin(ficheDto);
        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                "Bulletin de paie calcule avec succes",
                responseDto,
                HttpStatus.OK
        );
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }


    // cree un bulletin et le sauvegarde
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEUR')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> createBulletin(
            @RequestBody BulletinPaieCreateDto dto
    ) {
        BulletinPaieResponseDto bulletinCalcule = bulletinPaieService.saveBulletinPaie(dto);
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


    @GetMapping("/employeur/paginated")
    public ResponseEntity<ApiResponse<Page<BulletinPaieEmployeurDto>>> getBulletinsForEmployeurPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search, // Recherche globale (nom/prénom/matricule)
            @RequestParam(required = false) List<StatusBulletin> statuts // Statuts à filtrer (ex: GENERE,VALIDE,ENVOYE)
    ) {
        // Par défaut si aucun statut n'est précisé, on montre les "actifs"
        if (statuts == null || statuts.isEmpty()) {
            statuts = Arrays.asList(StatusBulletin.GÉNÉRÉ, StatusBulletin.VALIDÉ, StatusBulletin.ENVOYÉ);
        }
        Page<BulletinPaieEmployeurDto> bulletinsPage = bulletinPaieService.getBulletinsForEmployerPaginated(page, size, search, statuts);

        ApiResponse<Page<BulletinPaieEmployeurDto>> response = new ApiResponse<>("Liste paginée récupérée", bulletinsPage,HttpStatus.OK);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<?> generatePdfEmploye(@PathVariable Long bulletinId) {
        try {
            Optional<BulletinPaieResponseDto> bulletinOptional = bulletinPaieService.getBulletinPaieById(bulletinId);

            if (bulletinOptional.isEmpty()) {
                logger.error("Bulletin non trouvé avec l'ID: {}", bulletinId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Bulletin non trouvé", null, HttpStatus.NOT_FOUND));
            }

            BulletinPaieResponseDto bulletinCalcul = bulletinOptional.get();

            if (bulletinCalcul.getEmploye() == null) {
                logger.error("Données d'employé manquantes pour le bulletin: {}", bulletinId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>("Données d'employé manquantes", null, HttpStatus.BAD_REQUEST));
            }

            byte[] pdfBytes = pdfService.generateBulletinPdf(bulletinCalcul);

            if (pdfBytes == null || pdfBytes.length == 0) {
                logger.error("Échec de la génération du PDF pour le bulletin: {}", bulletinId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>("Échec de la génération du PDF", null, HttpStatus.INTERNAL_SERVER_ERROR));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = String.format("bulletin_paie_%s_%s_%s.pdf",
                    bulletinCalcul.getEmploye().getNom(),
                    bulletinCalcul.getEmploye().getPrenom(),
                    bulletinCalcul.getEmploye().getMatricule());

            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du PDF pour le bulletin {}: {}", bulletinId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erreur lors de la génération du PDF", null, HttpStatus.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la génération du PDF pour le bulletin {}: {}", bulletinId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Erreur inattendue lors de la génération du PDF", null, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }


    @GetMapping(value = "/{id}/previews", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEUR') or (hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#id)))")
    public ResponseEntity<String> getBulletinHtml(@PathVariable Long id) {
        logger.info("Début de la génération HTML pour le bulletin ID: {}", id);

        try {
            if (id == null || id <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<html><body><h1>ID invalide</h1></body></html>");
            }

            // Récupérer l'entité BulletinPaie complète
            Optional<BulletinPaie> bulletinOpt = bulletinPaieRepo
                    .findByIdWithEverything(id);
            if (bulletinOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("<html><body><h1>Bulletin non trouvé</h1></body></html>");
            }
            BulletinPaie bulletin = bulletinOpt.get();

            // ❌ SUPPRIME cette ligne : pas de recalcul !
            // BulletinPaie bulletinCalcule = bulletinPaieService.calculBulletin(bulletin);

            // Mapper vers DTO à jour (valeurs stockées)
            BulletinPaieResponseDto bulletinData = bulletinPaieService.convertToDto(bulletin);

            if (bulletinData.getEmploye() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<html><body><h1>Données d'employé manquantes</h1></body></html>");
            }

            String html = pdfService.generateHtmlContentForPdf(bulletinData);

            if (html == null || html.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("<html><body><h1>Erreur lors de la génération HTML</h1></body></html>");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du HTML pour le bulletin {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>Erreur lors de la génération du HTML</h1><p>" +
                            e.getMessage() + "</p></body></html>");
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





}
