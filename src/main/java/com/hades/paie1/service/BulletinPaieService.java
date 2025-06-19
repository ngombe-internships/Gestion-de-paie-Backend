package com.hades.paie1.service;

import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.BulletinPaieRepo;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.UserRepository;
import com.hades.paie1.service.calculators.CotisationCalculator;
import com.hades.paie1.service.calculators.ImpotCalculator;
import com.hades.paie1.service.calculators.SalaireCalculator;
import com.hades.paie1.utils.MathUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BulletinPaieService {

    public BulletinPaieRepo bulletinRepo;
    private EmployeRepository employeRepo;

    private CotisationCalculator cotisationCalculator;
    private ImpotCalculator impotCalculator;
    private SalaireCalculator calculator;
    private MathUtils mathUtils;
    private  EmployeService employeService;
    private UserRepository userRepository;
    public BulletinPaieService (
            CotisationCalculator cotisationCalculator,
            ImpotCalculator impotCalculator,
            SalaireCalculator calculator,
            MathUtils mathUtils,
            BulletinPaieRepo bulletinRepo,
            EmployeRepository employeRepo,
            EmployeService employeService,
            UserRepository userRepository


    ){
       this.calculator = calculator;
       this.mathUtils = mathUtils;
       this.cotisationCalculator = cotisationCalculator;
       this.impotCalculator = impotCalculator;
       this.bulletinRepo= bulletinRepo;
       this.employeRepo= employeRepo;
       this.employeService= employeService;
       this.userRepository = userRepository;
    }
    //methode pour calculer employer avec son bulletin
    public BulletinPaie calculBulletin (BulletinPaie fiche){

        Employe employe = employeRepo.findById(fiche.getEmploye().getId())
                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouve avec l'ID : " +fiche.getEmploye().getId()));


        fiche.setEmploye(employe);

        fiche.setSalaireBase(calculSalaireBase(fiche));
        fiche.setHeuresSup1(calculHeureSup1(fiche));
        fiche.setHeuresSup2(calculHeureSup2(fiche));
        fiche.setHeuresFerie(calculHeureFerie(fiche));
        fiche.setHeuresNuit(calculHeureNuit(fiche));
        fiche.setPrimeTransport(fiche.getPrimeTransport());
        fiche.setPrimePonctualite(fiche.getPrimePonctualite());
        fiche.setPrimeAnciennete(fiche.getPrimeAnciennete());
        fiche.setPrimeRendement(fiche.getPrimeRendement());
        fiche.setPrimeTechnicite(fiche.getPrimeTechnicite());
        fiche.setTotalPrimes(calculTotalPrimes(fiche));

        fiche.setSalaireBrut(calculator.calculSalaireBrut(fiche));
        fiche.setSalaireImposable(calculator.calculSalaireImposable(fiche));
        fiche.setBaseCnps(calculator.calculBaseCnps(fiche));


        // === SECTION IMPÔTS ET TAXES ===
        fiche.setIrpp(impotCalculator.calculIrpp(fiche));
        fiche.setCac(impotCalculator.calculCac(fiche));
        fiche.setTaxeCommunale(impotCalculator.calculTaxeCommunal(fiche));
        fiche.setRedevanceAudioVisuelle(impotCalculator.calculRedevanceAudioVisuelle(fiche));

        // === SECTION COTISATIONS SALARIALES (RETENUES) ===
        fiche.setCnpsVieillesseSalarie(cotisationCalculator.calculCnpsVieillesseSalarie(fiche));
        fiche.setCreditFoncierSalarie(cotisationCalculator.calculCreditFoncierSalarie(fiche));
        fiche.setFneSalarie(cotisationCalculator.calculFneSalaire(fiche));
        fiche.setTotalRetenues(cotisationCalculator.calculTotalRetenuesSalaire(fiche));

        // === SECTION CHARGES PATRONALES ===
        fiche.setCnpsVieillesseEmployeur(cotisationCalculator.calculCnpsVieillesseEmployeur(fiche));
        fiche.setCnpsAllocationsFamiliales(cotisationCalculator.calculCnpsAllocationsFamiliales(fiche));
        fiche.setCnpsAccidentsTravail(cotisationCalculator.calculCnpsAccidentsTravail(fiche));
        fiche.setCreditFoncierPatronal(cotisationCalculator.calculCreditFoncierPatronal(fiche));
        fiche.setFnePatronal(cotisationCalculator.calculFnePatronal(fiche));
        fiche.setTotalChargesPatronales(cotisationCalculator.calculTotalChargesPatronales(fiche));

        // === SECTION TOTAUX FINAUX ===
        fiche.setSalaireNet(calculSalaireNet(fiche));
        fiche.setCoutTotalEmployeur(calculCoutTotalEmployeur(fiche));
        fiche.setCotisationCnps(cotisationCalculator.cotisationCnps(fiche));


        return fiche;
    }



    public BulletinPaieResponseDto convertToDto(BulletinPaie bulletinPaie) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();

        dto.setTauxHoraire(bulletinPaie.getTauxHoraire());
        dto.setHeuresNormal(bulletinPaie.getHeuresNormal());
        dto.setSalaireBase(bulletinPaie.getSalaireBase());
        dto.setHeureSup1(bulletinPaie.getHeuresSup1());
        dto.setHeureSup2(bulletinPaie.getHeuresSup2());
        dto.setHeureNuit(bulletinPaie.getHeuresNuit());
        dto.setHeureFerie(bulletinPaie.getHeuresFerie());
        dto.setPrimeTransport(bulletinPaie.getPrimeTransport());
        dto.setPrimePonctualite(bulletinPaie.getPrimePonctualite());
        dto.setPrimeAnciennete(bulletinPaie.getPrimeAnciennete());
        dto.setPrimeRendement(bulletinPaie.getPrimeRendement());
        dto.setPrimeTechnicite(bulletinPaie.getPrimeTechnicite());
        dto.setTotalPrimes(bulletinPaie.getTotalPrimes());
        dto.setSalaireBrut(bulletinPaie.getSalaireBrut());
        dto.setBaseCnps(bulletinPaie.getBaseCnps());
        dto.setSalaireImposable(bulletinPaie.getSalaireImposable());

        dto.setIrpp(bulletinPaie.getIrpp());
        dto.setCac(bulletinPaie.getCac());
        dto.setTaxeCommunale(bulletinPaie.getTaxeCommunale());
        dto.setRedevanceAudioVisuelle(bulletinPaie.getRedevanceAudioVisuelle());
        dto.setCnpsVieillesseSalarie(bulletinPaie.getCnpsVieillesseSalarie());
        dto.setCreditFoncierSalarie(bulletinPaie.getCreditFoncierSalarie());
        dto.setFneSalarie(bulletinPaie.getFneSalarie());
        dto.setTotalRetenues(bulletinPaie.getTotalRetenues());
        dto.setCnpsVieillesseEmployeur(bulletinPaie.getCnpsVieillesseEmployeur());
        dto.setCnpsAllocationsFamiliales(bulletinPaie.getCnpsAllocationsFamiliales());
        dto.setCnpsAccidentsTravail(bulletinPaie.getCnpsAccidentsTravail());
        dto.setCreditFoncierPatronal(bulletinPaie.getCreditFoncierPatronal());
        dto.setFnePatronal(bulletinPaie.getFnePatronal());
        dto.setTotalChargesPatronales(bulletinPaie.getTotalChargesPatronales());
        dto.setSalaireNet(bulletinPaie.getSalaireNet());
        dto.setCoutTotalEmployeur(bulletinPaie.getCoutTotalEmployeur());
        dto.setCotisationCnps(bulletinPaie.getCotisationCnps());

        // Convertir et définir EmployeResponseDto
        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = employeService.convertToDto(bulletinPaie.getEmploye());
            dto.setEmploye(employeDto);
        }

        return dto;
    }

    //Methode Crud
    public BulletinPaieResponseDto saveBulletinPaie (BulletinPaie fiche){

        BulletinPaie calculatedAndFilledBulletin = calculBulletin(fiche);
        BulletinPaie savedBulletin = bulletinRepo.save(calculatedAndFilledBulletin);
        return  convertToDto(savedBulletin);
    }

    public List<BulletinPaieResponseDto> getAllBulletinsPaie() {

        return bulletinRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional <BulletinPaieResponseDto> getBulletinPaieById (Long id){
        return bulletinRepo.findById(id)
                .map(this::convertToDto);
    }

    public List<BulletinPaieResponseDto> getBulletinByEmployed(Long employeId){
        Employe employe = employeRepo.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID : " + employeId));
        return bulletinRepo.findByEmploye(employe).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BulletinPaieResponseDto updateBulletinPaie (Long id, BulletinPaie updatedBulletinPaie){

        BulletinPaie existingBulletinPaie = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID :  "+ id));

        existingBulletinPaie.setSalaireBase(updatedBulletinPaie.getSalaireBase());
        existingBulletinPaie.setTauxHoraire(updatedBulletinPaie.getTauxHoraire());
        existingBulletinPaie.setHeuresSup1(updatedBulletinPaie.getHeuresSup1());
        existingBulletinPaie.setHeuresSup2(updatedBulletinPaie.getHeuresSup2());
        existingBulletinPaie.setHeuresNuit(updatedBulletinPaie.getHeuresNuit());
        existingBulletinPaie.setHeuresFerie(updatedBulletinPaie.getHeuresFerie());
        existingBulletinPaie.setPrimeTransport(updatedBulletinPaie.getPrimeTransport());
        existingBulletinPaie.setPrimePonctualite(updatedBulletinPaie.getPrimePonctualite());
        existingBulletinPaie.setPrimeTechnicite(updatedBulletinPaie.getPrimeTechnicite());
        existingBulletinPaie.setPrimeAnciennete(updatedBulletinPaie.getPrimeAnciennete());
        existingBulletinPaie.setPrimeRendement(updatedBulletinPaie.getPrimeRendement());
        existingBulletinPaie.setAutrePrimes(updatedBulletinPaie.getAutrePrimes());
        existingBulletinPaie.setAvantageNature(updatedBulletinPaie.getAvantageNature());

        if(updatedBulletinPaie.getEmploye() != null && updatedBulletinPaie.getEmploye().getId() != null
                && !existingBulletinPaie.getEmploye().getId().equals(updatedBulletinPaie.getEmploye().getId())){
               Employe newEmploye = employeRepo.findById(updatedBulletinPaie.getEmploye().getId())
                       .orElseThrow(()-> new RessourceNotFoundException("Nouveau Employe non trouve avec id :" +updatedBulletinPaie.getEmploye().getId()));
               existingBulletinPaie.setEmploye(newEmploye);
        }

        BulletinPaie calculBulletinUpdate = calculBulletin(existingBulletinPaie);

        BulletinPaie savedBulletin = bulletinRepo.save(calculBulletinUpdate);
        return convertToDto(savedBulletin);
    }

    public void deleteBulletinPaie (Long id) {
        if (!bulletinRepo.existsById(id)){
            throw new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID :  "+ id);
        }
        bulletinRepo.deleteById(id);
    }

    public boolean isBulletinOfCurrentUser (Long bulletinId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        Optional<User> authenticatedUser = userRepository.findByUsername(username);

        if (authenticatedUser.isEmpty()) {
            return false;
        }

        User user = authenticatedUser.get();

        // L'utilisateur authentifié doit être lié à un employé pour cette vérification
        if (user.getEmploye() == null) {
            return false;
        }

        Long authenticatedEmployeId = user.getEmploye().getId();

        // Récupérer le bulletin de paie et vérifier si l'employé associé correspond à l'utilisateur authentifié
        return bulletinRepo.findById(bulletinId)
                .map(bulletin -> bulletin.getEmploye() != null && bulletin.getEmploye().getId().equals(authenticatedEmployeId))
                .orElse(false);
    }

    public List<BulletinPaieResponseDto> getBulletinsFotCurrentUser() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
              throw new IllegalArgumentException("Aucun utilisateur authentifie");
          }
          String username = authentication.getName();

          Optional<User> currentUserOptinal = userRepository.findByUsername(username);

          if (currentUserOptinal.isEmpty()) {
              throw new RessourceNotFoundException("Utilisateur non trouve avec le nom d'utilisateur: " + username);
          }
          User currentUser = currentUserOptinal.get();

          if (currentUser.getRole().name().equals("ADMIN")) {
              return bulletinRepo.findAll().stream()
                      .map(this::convertToDto)
                      .collect(Collectors.toList());
          } else if (currentUser.getRole().name().equals("EMPLOYE")) {
              if (currentUser.getEmploye() == null) {
                  throw new IllegalStateException("Le compte employe n'est pas lie a un enregistrement d'employe.");
              }
              Employe employe = currentUser.getEmploye();
              return bulletinRepo.findByEmploye(employe).stream()
                      .map(this::convertToDto)
                      .collect(Collectors.toList());
          } else {
              throw new IllegalStateException("Role d'utilisateur non pris en charge pour la recuperation des bulletins");
          }

    }









    //Calculs salaire
    public BigDecimal calculSalaireBase(BulletinPaie fiche){
        return calculator.calculSalaireBase(fiche);
    }

    //ajout de allocation de conge
    public BigDecimal calculSalaireBrut(BulletinPaie fiche) {
        return calculator.calculSalaireBrut(fiche);
    }

    public BigDecimal calculSalaireImposable(BulletinPaie fiche) {
        return calculator.calculSalaireImposable(fiche);
    }

    //Calcul d'impots
    public BigDecimal calculIrpp(BulletinPaie fiche) {
        return impotCalculator.calculIrpp(fiche);
    }

    public BigDecimal calculTaxeCommunale(BulletinPaie fiche) {
        return impotCalculator.calculTaxeCommunal(fiche);
    }

    // Calculs de cotisations
    public BigDecimal calculTotalRetenues(BulletinPaie fiche) {
        return cotisationCalculator.calculTotalRetenuesSalaire(fiche);
    }

    public BigDecimal calculTotalChargesPatronales(BulletinPaie fiche) {
        return cotisationCalculator.calculTotalChargesPatronales(fiche);
    }

    // Calculs finaux
    public BigDecimal calculSalaireNet (BulletinPaie fiche) {
        BigDecimal salaireBrut = calculSalaireBrut(fiche);
        BigDecimal totalRetenues = calculTotalRetenues(fiche);
        return salaireBrut.subtract(totalRetenues).max(BigDecimal.ZERO);

    }

    public  BigDecimal calculCoutTotalEmployeur (BulletinPaie fiche) {
        BigDecimal salaireBrut = calculSalaireBrut(fiche);
        BigDecimal chargesPatronal = calculTotalChargesPatronales(fiche);
        return mathUtils.safeAdd(salaireBrut, chargesPatronal);
    }


    // Méthodes utilitaires pour accéder aux calculs détaillés
    public BigDecimal calculHeureSup1(BulletinPaie fiche) {
        return calculator.calculHeureSup1(fiche);
    }

    public BigDecimal calculHeureSup2(BulletinPaie fiche) {
        return calculator.calculHeureSup2(fiche);
    }

    public BigDecimal calculHeureNuit(BulletinPaie fiche) {
        return calculator.calculHeureNuit(fiche);
    }

    public BigDecimal calculHeureFerie(BulletinPaie fiche) {
        return calculator.calculHeureFerie(fiche);
    }

    public BigDecimal calculTotalPrimes(BulletinPaie fiche) {
        return calculator.calculTotalPrimes(fiche);
    }

    public BigDecimal calculCac(BulletinPaie fiche) {
        return impotCalculator.calculCac(fiche);
    }

    public BigDecimal calculRedevanceAudioVisuelle(BulletinPaie fiche) {
        return impotCalculator.calculRedevanceAudioVisuelle(fiche);
    }

    // Cotisations détaillées - salariales
    public BigDecimal calculCnpsVieillesseSalaire(BulletinPaie fiche) {
        return cotisationCalculator.calculCnpsVieillesseSalarie(fiche);
    }

    public BigDecimal calculCreditFoncierSalaire(BulletinPaie fiche) {
        return cotisationCalculator.calculCreditFoncierSalarie(fiche);
    }

    public BigDecimal calculFneSalaire(BulletinPaie fiche) {
        return cotisationCalculator.calculFneSalaire(fiche);
    }

    // Cotisations détaillées - patronales
    public BigDecimal calculCnpsVieillesseEmployeur(BulletinPaie fiche) {
        return cotisationCalculator.calculCnpsVieillesseEmployeur(fiche);
    }

    public BigDecimal calculCnpsAllocationsFamiliales(BulletinPaie fiche) {
        return cotisationCalculator.calculCnpsAllocationsFamiliales(fiche);
    }

    public BigDecimal calculCnpsAccidentsTravail(BulletinPaie fiche) {
        return cotisationCalculator.calculCnpsAccidentsTravail(fiche);
    }

    public BigDecimal calculCreditFoncierPatronal(BulletinPaie fiche) {
        return cotisationCalculator.calculCreditFoncierPatronal(fiche);
    }

    public BigDecimal calculFnePatronal(BulletinPaie fiche) {
        return cotisationCalculator.calculFnePatronal(fiche);
    }


    public BigDecimal calculBaseCnps(BulletinPaie fiche) {
        return calculator.calculBaseCnps(fiche);
    }


    // pour les calcul de la cnps on utilise ceci
//    public BigDecimal calculBaseCnps1(BulletinPaie fiche) {
//        return calculator.calculAllocationConge(fiche);
//    }

    public BigDecimal calculCotisationCnps (BulletinPaie fiche){ return  cotisationCalculator.cotisationCnps(fiche); }


}
