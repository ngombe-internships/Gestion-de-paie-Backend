package com.hades.paie1.service;

import com.hades.paie1.dto.BulletinPaieEmployeurDto;
import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.dto.EntrepriseDto;
import com.hades.paie1.enum1.Role;
import com.hades.paie1.enum1.StatusBulletin;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.BulletinPaie;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.Entreprise;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.BulletinPaieRepo;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.EntrepriseRepository;
import com.hades.paie1.repository.UserRepository;
import com.hades.paie1.service.calculators.CotisationCalculator;
import com.hades.paie1.service.calculators.ImpotCalculator;
import com.hades.paie1.service.calculators.SalaireCalculator;
import com.hades.paie1.utils.MathUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
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
    private EntrepriseRepository entrepriseRepository;

    public BulletinPaieService (
            CotisationCalculator cotisationCalculator,
            ImpotCalculator impotCalculator,
            SalaireCalculator calculator,
            MathUtils mathUtils,
            BulletinPaieRepo bulletinRepo,
            EmployeRepository employeRepo,
            EmployeService employeService,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository


    ){
       this.calculator = calculator;
       this.mathUtils = mathUtils;
       this.cotisationCalculator = cotisationCalculator;
       this.impotCalculator = impotCalculator;
       this.bulletinRepo= bulletinRepo;
       this.employeRepo= employeRepo;
       this.employeService= employeService;
       this.userRepository = userRepository;
       this.entrepriseRepository = entrepriseRepository;
    }



    //pas encore utilise mais pemet a ce que employe pour qui le bulletin est cree appartie bien a entreprise
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceNotFoundException("User not found: " + username)); // Utilisez RessourceNotFoundException ou UsernameNotFoundException
    }






    //methode pour calculer employer avec son bulletin
    public BulletinPaie calculBulletin (BulletinPaie fiche){

        //Assurez-vous que entite employe est charge
        Employe employe = employeRepo.findById(fiche.getEmploye().getId())
                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouve avec l'ID : " +fiche.getEmploye().getId()));


        //charger entite entreprise
        Entreprise entreprise = entrepriseRepository.findById(fiche.getEntreprise().getId())
                        .orElseThrow(()-> new RessourceNotFoundException("Entreprise non trouver avec l'ID :" +fiche.getEntreprise().getId()));

        fiche.setEmploye(employe);
        fiche.setEntreprise(entreprise);


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
        fiche.setPrimeExceptionnelle(fiche.getPrimeExceptionnelle());
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

        fiche.setDateCreationBulletin(LocalDate.now()); // Date de création actuelle
        fiche.setAnnee(LocalDate.now().getYear()); // Année actuelle
        fiche.setMois(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)); // Nom du mois actuel (ex: "juin")

        // Définissez un statut initial. C'est CRUCIAL pour que les boutons s'affichent.
        fiche.setStatusBulletin(StatusBulletin.GÉNÉRÉ);

        return fiche;
    }



    public BulletinPaieResponseDto convertToDto(BulletinPaie bulletinPaie) {
        BulletinPaieResponseDto dto = new BulletinPaieResponseDto();
        dto.setId(bulletinPaie.getId());
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

        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());
        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());

        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee()!= null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A"); // Ou une autre valeur par défaut
        }


        //Mappez les donne de entrepise
        if(bulletinPaie.getEntreprise() != null){
            EntrepriseDto entrepriseDto = new EntrepriseDto();
            entrepriseDto.setId(bulletinPaie.getEntreprise().getId());
            entrepriseDto.setNom(bulletinPaie.getEntreprise().getNom());
            entrepriseDto.setNumeroSiret(bulletinPaie.getEntreprise().getNumeroSiret());
            entrepriseDto.setAdresseEntreprise(bulletinPaie.getEntreprise().getAdresseEntreprise());
            entrepriseDto.setTelephoneEntreprise(bulletinPaie.getEntreprise().getTelephoneEntreprise());
            entrepriseDto.setEmailEntreprise(bulletinPaie.getEntreprise().getEmailEntreprise());
            entrepriseDto.setLogoUrl(bulletinPaie.getEntreprise().getLogoUrl());

            System.out.println("Logo URL dans EntrepriseDto (avant de passer au template) : " + entrepriseDto.getLogoUrl());

            dto.setEntreprise(entrepriseDto);
        }


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

    //pour gerer afficher en fonction  des roles
    public BulletinPaieResponseDto getBulletinById(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouve avec l'id: " + id));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve avec le nom: " + currentUsername));
        if (currentUser.getRole() == Role.EMPLOYE) {
            Employe employe = employeRepo.findByUser(currentUser)
                    .orElseThrow(() -> new RessourceNotFoundException("No employee profile found for user: " + currentUsername));
            if (!bulletin.getEmploye().getId().equals(employe.getId())) {
                throw new AccessDeniedException("You are not authorized to view this bulletin.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            Entreprise entreprise = currentUser.getEntreprise();
            if (entreprise == null) {
                throw new IllegalStateException("Authenticated employer is not associated with an enterprise.");
            }
            if (!bulletin.getEntreprise().getId().equals(entreprise.getId())) {
                throw new AccessDeniedException("You are not authorized to view bulletins from another company.");
            }
        }

        return  convertToDto(bulletin);
    }



    public Optional <BulletinPaieResponseDto> getBulletinPaieById (Long id){
        return bulletinRepo.findById(id)
                .map(this::convertToDto);
    }

    //cherche un employe
    public List<BulletinPaieResponseDto> getBulletinByEmployed(Long employeId){
        Employe employe = employeRepo.findById(employeId)
                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID : " + employeId));
        return bulletinRepo.findByEmploye(employe).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    //mise a jour


    public void deleteBulletinPaie (Long id) {
        if (!bulletinRepo.existsById(id)){
            throw new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID :  "+ id);
        }
        bulletinRepo.deleteById(id);
    }

    public boolean isBulletinOfCurrentUser(Long bulletinId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false; // Pas d'utilisateur authentifié
        }

        String username = authentication.getName();
        Optional<User> authenticatedUser = userRepository.findByUsername(username);

        if (authenticatedUser.isEmpty()) {
            return false; // Utilisateur non trouvé dans la base de données
        }

        User user = authenticatedUser.get();

        // On vérifie le rôle de l'utilisateur
        // Votre entité User a un seul champ `role` de type `Role` (enum).
        // Donc, nous devons comparer directement l'enum.
        boolean isEmployer = user.getRole() == Role.EMPLOYEUR; // Vérifie si le rôle est EMPLOYEUR
        boolean isEmployeRole = user.getRole() == Role.EMPLOYE; // Vérifie si le rôle est EMPLOYE

        if (isEmployer) {
            // L'utilisateur est un EMPLOYEUR. Il doit être lié à une entreprise.
            if (user.getEntreprise() == null) {
                System.out.println("DEBUG ENTREPRISE LINK: L'utilisateur EMPLOYEUR '" + username + "' n'est PAS lié à une entité Entreprise.");
                return false; // Un EMPLOYEUR doit avoir une entreprise associée
            }

            Long authenticatedEntrepriseId = user.getEntreprise().getId();
            System.out.println("DEBUG ENTREPRISE ID: ID de l'entreprise liée à l'utilisateur EMPLOYEUR: " + authenticatedEntrepriseId);

            // Tente de trouver le bulletin de paie
            return bulletinRepo.findById(bulletinId)
                    .map(bulletin -> {
                        // Vérifie si le bulletin est lié à un employé
                        if (bulletin.getEmploye() == null) {
                            System.out.println("DEBUG BULLETIN EMPLOYE: Bulletin ID " + bulletinId + " trouvé, mais SANS employé associé.");
                            return false;
                        }
                        Employe employeDuBulletin = bulletin.getEmploye();

                        // Vérifie si l'employé du bulletin est lié à une entreprise
                        if (employeDuBulletin.getEntreprise() == null) {
                            System.out.println("DEBUG BULLETIN ENTREPRISE: L'employé du bulletin ID " + bulletinId + " n'est PAS lié à une entité Entreprise.");
                            return false;
                        }
                        Long bulletinEntrepriseId = employeDuBulletin.getEntreprise().getId();
                        System.out.println("DEBUG BULLETIN ENTREPRISE: Bulletin ID " + bulletinId + " est lié à l'entreprise ID: " + bulletinEntrepriseId);

                        // Compare l'ID de l'entreprise de l'employeur avec l'ID de l'entreprise de l'employé du bulletin
                        boolean match = bulletinEntrepriseId.equals(authenticatedEntrepriseId);
                        System.out.println("DEBUG MATCH: L'ID de l'entreprise du bulletin correspond à l'ID de l'entreprise authentifiée? " + match);
                        return match;
                    })
                    .orElseGet(() -> {
                        System.out.println("DEBUG BULLETIN NOT FOUND: Bulletin avec l'ID " + bulletinId + " non trouvé.");
                        return false; // Bulletin non trouvé
                    });
        }
        else if (isEmployeRole) {
            // L'utilisateur est un EMPLOYE. Il ne peut voir que son propre bulletin.
            // L'utilisateur EMPLOYE doit être lié à une entité Employe.
            if (user.getEmploye() == null) {
                System.out.println("DEBUG EMPLOYE LINK: L'utilisateur '" + username + "' avec le rôle EMPLOYE n'est PAS lié à une entité Employe.");
                return false;
            }
            Long authenticatedEmployeId = user.getEmploye().getId();
            System.out.println("DEBUG EMPLOYE ID: ID de l'employé lié à l'utilisateur EMPLOYE: " + authenticatedEmployeId);

            return bulletinRepo.findById(bulletinId)
                    .map(bulletin -> {
                        boolean match = bulletin.getEmploye() != null && bulletin.getEmploye().getId().equals(authenticatedEmployeId);
                        System.out.println("DEBUG MATCH: L'ID de l'employé du bulletin correspond à l'ID de l'employé authentifié? " + match);
                        return match;
                    })
                    .orElseGet(() -> {
                        System.out.println("DEBUG BULLETIN NOT FOUND: Bulletin avec l'ID " + bulletinId + " non trouvé.");
                        return false;
                    });
        }

        // Si l'utilisateur n'a ni le rôle EMPLOYEUR ni EMPLOYE (et pas ADMIN qui est géré par @PreAuthorize),
        // il n'a pas accès via cette méthode.
        System.out.println("DEBUG: L'utilisateur n'a pas les rôles ou la configuration nécessaire pour cette vérification.");
        return false;
    }

    public List<BulletinPaieEmployeurDto> getBulletinsForEmployer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.EMPLOYEUR) {
            throw new AccessDeniedException("Only employers can view their company's bulletins.");
        }

        Entreprise entreprise = currentUser.getEntreprise();
        if (entreprise == null) {
            throw new IllegalStateException("Authenticated employer is not associated with an enterprise.");
        }

        return bulletinRepo.findByEntreprise(entreprise).stream()
                .map(this::convertToEmployeurDto)
                .collect(Collectors.toList());
    }

    public List<BulletinPaieResponseDto> getMyBulletins() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.EMPLOYE) {
            throw new AccessDeniedException("Only employees can view their own bulletins.");
        }

        Employe employe = employeRepo.findByUser(currentUser)
                .orElseThrow(() -> new RessourceNotFoundException("No employee profile found for user: " + currentUsername));

        //Defini les statut qui doivent etre visibles par employe
        List<StatusBulletin> visibleStatuses = List.of(StatusBulletin.ENVOYÉ, StatusBulletin.ARCHIVÉ);

        return bulletinRepo.findByEmployeAndStatusBulletinIn(employe, visibleStatuses).stream()
                .map(this::convertToDto) // Using the existing convertToDto for employee's own bulletins
                .collect(Collectors.toList());
    }

    //Statut
    public BulletinPaieResponseDto validerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouve avec l'ID :" +id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw  new AccessDeniedException("Seuls les employeur ou admin peuvent valider un bulletin ");
        }

        if (currentUser.getRole() == Role.EMPLOYEUR &&!bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à valider les bulletins d'une autre entreprise.");
        }


        // Vérifier la transition de statut
        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("GÉNÉRÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être validé que s'il est au statut 'Généré'. Statut actuel : " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.VALIDÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    //enoye bulletin
    public BulletinPaieResponseDto envoyerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent envoyer un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à envoyer les bulletins d'une autre entreprise.");
        }

        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("VALIDÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être envoyé que s'il est au statut 'Validé'. Statut actuel : " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.ENVOYÉ);
        bulletin.setDatePaiement(LocalDate.now()); // Définir la date de paiement lors de l'envoi
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    public BulletinPaieResponseDto archiverBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent archiver un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à archiver les bulletins d'une autre entreprise.");
        }



        if (bulletin.getStatusBulletin() == null || !bulletin.getStatusBulletin().toString().trim().equalsIgnoreCase("ENVOYÉ")) {
            throw new IllegalStateException("Le bulletin ne peut être archivé que s'il est au statut 'Validé' ou 'Envoyé'. Statut actuel :: " + bulletin.getStatusBulletin());
        }

        bulletin.setStatusBulletin(StatusBulletin.ARCHIVÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }

    public BulletinPaieResponseDto annulerBulletin(Long id) {
        BulletinPaie bulletin = bulletinRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Bulletin de paie non trouvé avec l'ID : " + id));

        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != Role.EMPLOYEUR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les employeurs ou administrateurs peuvent annuler un bulletin.");
        }
        if (currentUser.getRole() == Role.EMPLOYEUR && !bulletin.getEntreprise().getId().equals(currentUser.getEntreprise().getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à annuler les bulletins d'une autre entreprise.");
        }

        if (bulletin.getStatusBulletin() != null && "ARCHIVÉ".equalsIgnoreCase(bulletin.getStatusBulletin().toString().trim())) {
            throw new IllegalStateException("Un bulletin archivé ne peut pas être annulé directement. Il doit être désarchivé ou une nouvelle rectification doit être créée.");
        }



        bulletin.setStatusBulletin(StatusBulletin.ANNULÉ);
        BulletinPaie savedBulletin = bulletinRepo.save(bulletin);
        return convertToDto(savedBulletin);
    }
























    private BulletinPaieEmployeurDto convertToEmployeurDto(BulletinPaie bulletinPaie) {
        BulletinPaieEmployeurDto dto = new BulletinPaieEmployeurDto();
        dto.setId(bulletinPaie.getId());
        dto.setSalaireBase(bulletinPaie.getSalaireBase());
        dto.setTauxHoraire(bulletinPaie.getTauxHoraire());
        dto.setHeuresNormal(bulletinPaie.getHeuresNormal());
        dto.setHeuresSup1(bulletinPaie.getHeuresSup1());
        dto.setHeuresSup2(bulletinPaie.getHeuresSup2());
        dto.setHeuresNuit(bulletinPaie.getHeuresNuit());
        dto.setHeuresFerie(bulletinPaie.getHeuresFerie());
        dto.setPrimeTransport(bulletinPaie.getPrimeTransport());
        dto.setPrimePonctualite(bulletinPaie.getPrimePonctualite());
        dto.setPrimeTechnicite(bulletinPaie.getPrimeTechnicite());
        dto.setPrimeAnciennete(bulletinPaie.getPrimeAnciennete());
        dto.setPrimeRendement(bulletinPaie.getPrimeRendement());
        dto.setAutrePrimes(bulletinPaie.getAutrePrimes());
        dto.setAvantageNature(bulletinPaie.getAvantageNature());
        dto.setPrimeExceptionnelle(bulletinPaie.getPrimeExceptionnelle());
        dto.setTotalPrimes(bulletinPaie.getTotalPrimes());
        dto.setSalaireBrut(bulletinPaie.getSalaireBrut());
        dto.setSalaireImposable(bulletinPaie.getSalaireImposable());
        dto.setBaseCnps(bulletinPaie.getBaseCnps());
        dto.setBaseCnps1(bulletinPaie.getBaseCnps());
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
        dto.setCotisationCnps(bulletinPaie.getCotisationCnps());
        dto.setCoutTotalEmployeur(bulletinPaie.getCoutTotalEmployeur());
        dto.setSalaireNet(bulletinPaie.getSalaireNet());
//        dto.setDateCreation(bulletinPaie.getDateEmbauche());
//        dto.setPeriodeDebut(bulletinPaie.getPeriodeDebut());
//        dto.setPeriodeFin(bulletinPaie.getPeriodeFin());
//        dto.setJourConge(bulletinPaie.getJourConge());
//        dto.setMois(bulletinPaie.getMois());
//        dto.setAnnee(bulletinPaie.getAnnee());
        dto.setPeriodePaie(dto.getPeriodePaie());
      ;
        dto.setDateCreation(dto.getDateCreation());
        dto.setDatePaiement(dto.getDatePaiement());

        // Convert Employe to EmployeResponseDto
        if (bulletinPaie.getEmploye() != null) {
            EmployeResponseDto employeDto = new EmployeResponseDto();
            employeDto.setId(bulletinPaie.getEmploye().getId());
            employeDto.setNom(bulletinPaie.getEmploye().getNom());
            employeDto.setPrenom(bulletinPaie.getEmploye().getPrenom());
            employeDto.setMatricule(bulletinPaie.getEmploye().getMatricule());
            employeDto.setPoste(bulletinPaie.getEmploye().getPoste());
            // Add other necessary employee fields
            dto.setEmploye(employeDto);
        }

        dto.setDateCreationBulletin(bulletinPaie.getDateCreationBulletin());
        dto.setDatePaiement(bulletinPaie.getDatePaiement());
        dto.setStatusBulletin(bulletinPaie.getStatusBulletin());// Le statut est maintenant renseigné

        // Concaténez mois et année pour periodePaie (si votre DTO a ce champ pour l'affichage)
        if (bulletinPaie.getMois() != null && bulletinPaie.getAnnee()!= null) {
            dto.setPeriodePaie(bulletinPaie.getMois() + " " + bulletinPaie.getAnnee());
        } else {
            dto.setPeriodePaie("N/A"); // Ou une autre valeur par défaut
        }


        return dto;
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
}
