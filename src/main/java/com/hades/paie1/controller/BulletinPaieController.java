package com.hades.paie1.controller;

import com.hades.paie1.dto.ApiResponse;
import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.service.BulletinPaieService;
import com.hades.paie1.service.PdfService;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bulletins")

@CrossOrigin(origins = "http://localhost:4200")
public class BulletinPaieController {

    private BulletinPaieService bulletinPaieService;
    private PdfService pdfService;
    public  BulletinPaieController(BulletinPaieService bulletinPaieService , PdfService pdf) {
        this.bulletinPaieService = bulletinPaieService;
        this.pdfService = pdf;
    }


    @PostMapping("/calculate1")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> calculerBulletin1 (@RequestBody BulletinPaie fiche ){

        BulletinPaie calculBulletin = bulletinPaieService.calculBulletin(fiche);

        BulletinPaieResponseDto responseDto = bulletinPaieService.convertToDto(calculBulletin);
        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(

                "Bullerin de paie calcule avec succes" ,
                responseDto,
                HttpStatus.OK
        );
        return  new ResponseEntity<>(response, HttpStatus.OK);

    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> createBulletin(@RequestBody BulletinPaie fiche){
        BulletinPaieResponseDto bulletinCalcule = bulletinPaieService.saveBulletinPaie(fiche);
        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                "Bulletin de paie calcule  et sauvegarde avec succes",
                bulletinCalcule,
                HttpStatus.CREATED
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDto>>> getAllBulletins(){

        List<BulletinPaieResponseDto> bulletins = bulletinPaieService.getAllBulletinsPaie();
        ApiResponse<List<BulletinPaieResponseDto>> response = new ApiResponse<>(
                "Liste de tous les bulletins de paie",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYE')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDto>>> getBulletinsForUserRole(){
        List<BulletinPaieResponseDto> bulletins = bulletinPaieService.getAllBulletinsPaie();
        ApiResponse<List<BulletinPaieResponseDto>> response = new ApiResponse<>(
                "Liste des les bulletins de paie",
                bulletins,
                HttpStatus.OK
        );
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#id))")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDto>> updateBulletin (@PathVariable Long id , @RequestBody BulletinPaie fiche){
        BulletinPaieResponseDto bulletinUpdate = bulletinPaieService.updateBulletinPaie(id,fiche);

        ApiResponse<BulletinPaieResponseDto> response = new ApiResponse<>(
                "Bulletin de paie mis a jour avec succes",
                bulletinUpdate,
                HttpStatus.OK
        );
        return  new ResponseEntity<>(response, HttpStatus.OK);

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBulletin(@PathVariable Long id){
        bulletinPaieService.deleteBulletinPaie(id);
        ApiResponse<Void> response = new ApiResponse<>(
                "Bulletin de paie supprime avec succes",
                null,
                HttpStatus.NO_CONTENT

        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }


    //generer pdf

    @PostMapping("/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public  ResponseEntity<byte[]> generatePdf(@RequestBody BulletinPaie fiche) {
        try{
            BulletinPaieResponseDto bulletinCalcule = calculBulletin(fiche);

            byte[] pdfBytes = pdfService.generateBulletinPdf(bulletinCalcule);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment","bulletin_paie_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+ ".pdf" );

            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        }catch (DocumentException | IOException e){
            e.printStackTrace();
            return  new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    @GetMapping("/pdf/{bulletinId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYE') and @bulletinPaieService.isBulletinOfCurrentUser(#bulletinPaie.employe.id))")

    public  ResponseEntity<byte[]> generatePdfEmploye(@PathVariable Long bulletinId){

        try{
            Optional<BulletinPaieResponseDto> bulletinOptional = bulletinPaieService.getBulletinPaieById(bulletinId);

            if (bulletinOptional.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            BulletinPaieResponseDto bulletinCalcul = bulletinOptional.get();

            byte[] pdfBytes = pdfService.generateBulletinPdf(bulletinCalcul);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "bulletin_paie" +bulletinCalcul.getEmploye().getNom()
                    +"_" + bulletinCalcul.getEmploye().getPrenom() +"_" + bulletinCalcul.getEmploye().getMatricule() + ".pdf";

            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e){
                    e.printStackTrace();
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
