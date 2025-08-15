package com.hades.maalipo.service;

import com.hades.maalipo.dto.employe.EmployeCreateDto;
import com.hades.maalipo.dto.employe.EmployeResponseDto;
import com.hades.maalipo.enum1.Role;
import com.hades.maalipo.exception.RessourceNotFoundException;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.Entreprise;
import com.hades.maalipo.model.User;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.UserRepository;
import com.hades.maalipo.service.calculators.AncienneteService;
import com.hades.maalipo.utils.PaieConstants;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeService {

    private   final EmployeRepository employeRepo;
    private  final  UserRepository userRepository;

    private  final  AncienneteService ancienneteService;
    private final AuditLogService auditLogService;


    private static  final Logger logger = LoggerFactory.getLogger(EmployeService.class);
    public EmployeService (EmployeRepository employeRepo, UserRepository userRepository, AncienneteService ancienneteService, AuditLogService auditLogService){
        this.employeRepo = employeRepo;
        this.userRepository = userRepository;
        this.ancienneteService = ancienneteService;
        this.auditLogService = auditLogService;
    }


    // Méthode utilitaire pour obtenir l'utilisateur authentifié
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    //methode pour convertir employeCreateDto en entite Employe
    public Employe convertToEntity(EmployeCreateDto dto) {

       return Employe.builder()
               .matricule(dto.getMatricule())
               .nom(dto.getNom())
               .prenom(dto.getPrenom())
               .numeroCnps(dto.getNumeroCnps())
               .niu(dto.getNiu())
               .telephone(dto.getTelephone())
               .actif(dto.getActif() != null ? dto.getActif() : true)
               .email(dto.getEmail())
               .adresse(dto.getAdresse())
               .dateEmbauche(dto.getDateEmbauche())
               .poste(dto.getPoste())
               .service(dto.getService())
               .classificationProfessionnelle(dto.getClassificationProfessionnelle())
               .categorieEnum(dto.getCategorie())
               .echelonEnum(dto.getEchelon())
               .typeContratEnum(dto.getTypeContratEnum())
               .dateNaissance(dto.getDateNaissance())
               .sexe(dto.getSexe())
               .soldeJoursConge(dto.getSoldeJoursConge())
               .heuresContractuellesHebdomadaires(dto.getHeuresContractuellesHebdomadaires())
               .joursOuvrablesContractuelsHebdomadaires(dto.getJoursOuvrablesContractuelsHebdomadaires())
               .salaireBase(dto.getSalaireBase())
               .build();

    }

    // Methode pour convertir Entite Employe en Employe Respose
    public EmployeResponseDto convertToDto(Employe employe) {
        if(employe == null){
            return null;
        }
        return EmployeResponseDto.builder()
                .id(employe.getId())
                .matricule(employe.getMatricule())
                .nom(employe.getNom())
                .prenom(employe.getPrenom())
                .numeroCnps(employe.getNumeroCnps())
                .niu(employe.getNiu())
                .telephone(employe.getTelephone())
                .email(employe.getEmail())
                .adresse(employe.getAdresse())
                .dateEmbauche(employe.getDateEmbauche())
                .service(employe.getService())
                .poste(employe.getPoste())
                .classificationProfessionnelle(employe.getClassificationProfessionnelle())
                .categorie(employe.getCategorieEnum())
                .echelon(employe.getEchelonEnum())
                .typeContratEnum(employe.getTypeContratEnum())
                .sexe(employe.getSexe())
                .dateNaissance(employe.getDateNaissance())
                .soldeJoursConge(employe.getSoldeJoursConge())
                .heuresContractuellesHebdomadaires(employe.getHeuresContractuellesHebdomadaires())
                .joursOuvrablesContractuelsHebdomadaires(employe.getJoursOuvrablesContractuelsHebdomadaires())
                .salaireBase(employe.getSalaireBase())
                .build();

    }

    @Transactional
    public EmployeResponseDto createEmploye (EmployeCreateDto employeDto){

        User currentUser = getAuthenticatedUser();
        // seul un employeur peut cree un role
        if(currentUser.getRole() != Role.EMPLOYEUR){
            throw  new AccessDeniedException("Seul un employeur est autorise a cree un employe ");
        }
        // un employeur doit etre lie a une entreprise pour cree une entreprise
        if(currentUser.getEntreprise() == null) {
            throw new AccessDeniedException("L'employeur n'est pas associe a une entreprise.");
        }

        if (employeRepo.existsByMatricule(employeDto.getMatricule())){
           throw new RessourceNotFoundException ("Matricule deja utilise !");
       }
       if(employeDto.getNumeroCnps() != null && employeRepo.existsByNumeroCnps(employeDto.getNumeroCnps())){
           throw new RessourceNotFoundException("Numero CNPS deja utilise !");
       }
       if(employeDto.getNiu() !=null && employeRepo.existsByNiu(employeDto.getNiu())){
           throw new RessourceNotFoundException("NIU deja utilise");
       }
       if(employeDto.getEmail() != null && employeRepo.existsByEmail(employeDto.getEmail())){
           throw new RessourceNotFoundException("Email deja utilise");
       }

       Employe employe = convertToEntity(employeDto);

       employe.setEntreprise(currentUser.getEntreprise());

       employe.setActif(true);

        Employe savedEmploye = employeRepo.save(employe);

       auditLogService.logAction(
                "CREATE_EMPLOYE",
                "Employe",
                savedEmploye.getId(),
                getAuthenticatedUser().getUsername(),
                "Création de l'employé " + savedEmploye.getNom()
        );

       return convertToDto(savedEmploye);

    }

    @Transactional
    public  EmployeResponseDto updateEmploye(EmployeCreateDto employeDto , Long id){
        Employe existingEmploye = employeRepo.findById(id).
                orElseThrow(() -> new RessourceNotFoundException("Employe non trouve avec ID :" +id));

        User currentUser = getAuthenticatedUser();

        //verification des autorisations
        if(currentUser.getRole() == Role.EMPLOYEUR) {
            if (currentUser.getEntreprise() == null || !existingEmploye.getEntreprise().equals(currentUser.getEntreprise())) {
                throw new AccessDeniedException("Vous n'etes pas autorise a modifier cet employe.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYE) {
            if (!existingEmploye.getUser().equals(currentUser)){
                throw new AccessDeniedException("Vous n'etes pas autorise a modifier cet employe.");
            }
        }

        if ( !existingEmploye.getMatricule().equals(employeDto.getMatricule()) && employeRepo.existsByMatricule(employeDto.getMatricule())){
            throw new RessourceNotFoundException ("Matricule deja utilise " + employeDto.getMatricule());
        }
        if(employeDto.getNumeroCnps() != null &&
                !employeDto.getNumeroCnps().equals(existingEmploye.getNumeroCnps()) &&
                employeRepo.existsByNumeroCnps(employeDto.getNumeroCnps())){
            throw new RessourceNotFoundException("Numero CNPS deja utilise " +employeDto.getNumeroCnps());
        }
        if(employeDto.getNiu() !=null &&
                !employeDto.getNiu().equals(existingEmploye.getNiu())&&
                employeRepo.existsByNiu(employeDto.getNiu())){
            throw new RessourceNotFoundException("NIU deja utilise" +employeDto.getNiu());
        }
        if(employeDto.getEmail() != null &&
                !employeDto.getEmail().equals(existingEmploye.getEmail())&&
                employeRepo.existsByEmail(employeDto.getEmail())){
            throw new RessourceNotFoundException("Email deja utilise" +employeDto.getEmail());
        }



        existingEmploye.setNom(employeDto.getNom());
        existingEmploye.setPrenom(employeDto.getPrenom());
        existingEmploye.setMatricule(employeDto.getMatricule());
        existingEmploye.setNumeroCnps(employeDto.getNumeroCnps());
        existingEmploye.setNiu(employeDto.getNiu());
        existingEmploye.setTelephone(employeDto.getTelephone());
        existingEmploye.setEmail(employeDto.getEmail());
        existingEmploye.setAdresse(employeDto.getAdresse());
        existingEmploye.setPoste(employeDto.getPoste());
        existingEmploye.setService(employeDto.getService());
        existingEmploye.setClassificationProfessionnelle(employeDto.getClassificationProfessionnelle());
        existingEmploye.setEchelonEnum(employeDto.getEchelon());
        existingEmploye.setCategorieEnum(employeDto.getCategorie());
        existingEmploye.setDateEmbauche(employeDto.getDateEmbauche());
        existingEmploye.setTypeContratEnum(employeDto.getTypeContratEnum());
        existingEmploye.setDateNaissance(employeDto.getDateNaissance());
        existingEmploye.setSexe(employeDto.getSexe());
        existingEmploye.setSoldeJoursConge(employeDto.getSoldeJoursConge());
        existingEmploye.setJoursOuvrablesContractuelsHebdomadaires(employeDto.getJoursOuvrablesContractuelsHebdomadaires());
        existingEmploye.setHeuresContractuellesHebdomadaires(employeDto.getHeuresContractuellesHebdomadaires());
        existingEmploye.setSalaireBase(employeDto.getSalaireBase());


        Employe savedEmploye = employeRepo.save(existingEmploye);

        auditLogService.logAction(
                "UPDATE_EMPLOYE",
                "Employe",
                savedEmploye.getId(),
                getAuthenticatedUser().getUsername(),
                "Mise à jour de l'employé " + savedEmploye.getNom()
        );
        return convertToDto(savedEmploye);
    }




    public List<EmployeResponseDto> getAllEmploye() {
        User currentUser = getAuthenticatedUser();
        List<Employe> employes;

        if (currentUser.getRole() == Role.ADMIN) {
            employes = employeRepo.findAll();
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            if (currentUser.getEntreprise() == null) {
                employes = Collections.emptyList();
            } else {
                employes = employeRepo.findByEntreprise(currentUser.getEntreprise());
            }
        } else if (currentUser.getRole() == Role.EMPLOYE) {
            if (currentUser.getEmploye() != null && currentUser.getEmploye().getEntreprise() != null) {
                employes = employeRepo.findByUser(currentUser).map(List::of).orElse(Collections.emptyList());
            } else {
                employes = Collections.emptyList();
            }
        } else {
            employes = Collections.emptyList();
        }

        return employes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<EmployeResponseDto> getAllEmployeForRegister() {
        return getAllEmploye();
    }

    public Page<EmployeResponseDto> getAllEmploye(int page, int size) { // Cette méthode n'a pas de searchTerm en paramètre
        User currentUser = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page,size);
        Page<Employe> employes;

        if (currentUser.getRole() == Role.ADMIN) {

            employes = employeRepo.findAll(pageable);
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            if (currentUser.getEntreprise() == null) {

                employes = Page.empty(pageable);
            } else {

                employes = employeRepo.findByEntrepriseOrderById(currentUser.getEntreprise(),pageable);
            }
        } else if (currentUser.getRole() == Role.EMPLOYE) {
            if (currentUser.getEmploye() != null && currentUser.getEmploye().getEntreprise() != null) {
                Employe employe = employeRepo.findByUser(currentUser).orElse(null);
                List<Employe> employeList = employe != null ? List.of(employe) : Collections.emptyList();

                employes = new PageImpl<>(employeList,pageable,employeList.size());
            } else {
                employes = Page.empty(pageable);
            }
        } else {
            employes = Page.empty(pageable);
        }


        return employes.map(this::convertToDto);
    }

    @Transactional
    public Optional <EmployeResponseDto> getEmployeById(Long id){
        User currentUser = getAuthenticatedUser();

        Optional<Employe> employeOptional = employeRepo.findById(id);
        if (employeOptional.isEmpty()) {
            return Optional.empty(); // Employé non trouvé
        }
        Employe employe = employeOptional.get();

        // Vérification des autorisations multi-tenant
        if (currentUser.getRole() == Role.ADMIN) {
            return Optional.of(convertToDto(employe)); // ADMIN peut voir n'importe quel employé
        } else if (currentUser.getRole() == Role.EMPLOYEUR) {
            if (currentUser.getEntreprise() == null || !employe.getEntreprise().equals(currentUser.getEntreprise())) {
                throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cet employé.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYE) {
            if (currentUser.getEmploye() == null || !employe.equals(currentUser.getEmploye())) {
                throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cet employé.");
            }
        } else {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à cet employé.");
        }
        return Optional.of(convertToDto(employe));
    }


    @Transactional
    public  void deleteEmploye (Long id){
        Employe employeToDelete = employeRepo.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Employe non trouvé avec ID " + id));

        User currentUser = getAuthenticatedUser();

        // Vérification des autorisations multi-tenant
        if (currentUser.getRole() == Role.EMPLOYEUR) {
            if (currentUser.getEntreprise() == null || !employeToDelete.getEntreprise().equals(currentUser.getEntreprise())) {
                throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cet employé.");
            }
        } else if (currentUser.getRole() == Role.EMPLOYE) {
            throw new AccessDeniedException("Un employé n'est pas autorisé à supprimer des employés.");
        }

        auditLogService.logAction(
                "DELETE_EMPLOYE",
                "Employe",
                employeToDelete.getId(),
                getAuthenticatedUser().getUsername(),
                "Suppression de l'employé " + employeToDelete.getNom()
        );
        employeRepo.delete(employeToDelete);
    }

    @Transactional
    public  List<EmployeResponseDto> searchEmployes(String searchTerm) {
        User currentUser = getAuthenticatedUser();
        List<Employe> employes;

        employes = employeRepo.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtrage multi-tenant
            if (currentUser.getRole() == Role.EMPLOYEUR) {
                if (currentUser.getEntreprise() == null) {
                    return cb.disjunction(); // Pas d'entreprise, pas de résultats
                }
                predicates.add(cb.equal(root.get("entreprise"), currentUser.getEntreprise()));
            } else if (currentUser.getRole() == Role.EMPLOYE) {
                if (currentUser.getEmploye() == null || currentUser.getEmploye().getEntreprise() == null) {
                    return cb.disjunction(); // Pas d'employé ou d'entreprise, pas de résultats
                }
                predicates.add(cb.equal(root.get("id"), currentUser.getEmploye().getId())); // Un employé ne peut chercher que son propre profil
            }
            // ADMIN n'ajoute pas de prédicat sur l'entreprise, il voit tout

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String lowerCaseSearchTerm = "%" + searchTerm.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("nom")), lowerCaseSearchTerm),
                        cb.like(cb.lower(root.get("prenom")), lowerCaseSearchTerm),
                        cb.like(cb.lower(root.get("matricule")), lowerCaseSearchTerm)
                );
                predicates.add(searchPredicate);
            }

            if (!predicates.isEmpty()) {
                return cb.and(predicates.toArray(new Predicate[0]));
            } else {
                return cb.conjunction();
            }
        });

        return employes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeResponseDto getEmployeProfilByAuthenticateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(()-> new UsernameNotFoundException("Utilisateur non trouve" + currentUsername));
        Employe employe =employeRepo.findByUser(currentUser)
                .orElseThrow(() -> new RessourceNotFoundException("Aucun profil employé trouvé pour l'utilisateur: " + currentUsername));

        return convertToDto(employe);
    }


    @Transactional
    public long countEmployeForAuthenticatedEmployer(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUSer = userRepository.findByUsername(currentUsername)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: " +currentUsername));
        if(currentUSer.getRole() != Role.EMPLOYEUR && currentUSer.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only emplyers or admis can view bulletin count.");
        }
        if (currentUSer.getRole() == Role.ADMIN) {
            return employeRepo.count();
        } else {
            Entreprise entreprise = currentUSer.getEntreprise();
            if(entreprise == null) {
                throw new IllegalStateException("Authenticated employer is not associated with an enterprise");
            }
            return employeRepo.countByEntreprise(entreprise);
        }
    }

    @Transactional
    public void updateEmployeLeaveBalance (Employe employe) {
        if(employe.getDateEmbauche() == null){
            return;
        }

        int ancienneteEnAnnees = ancienneteService.calculAncienneteEnAnnees(employe.getDateEmbauche());

        BigDecimal jourBonusAnciennete = BigDecimal.ZERO;
        if (ancienneteEnAnnees >=5){
            jourBonusAnciennete = BigDecimal.valueOf(ancienneteEnAnnees)
                    .divide(BigDecimal.valueOf(5), 0, RoundingMode.FLOOR)
                    .multiply(BigDecimal.valueOf(2));
        }

        BigDecimal totalJoursCongesAnnuels = PaieConstants.JOURCONGESBASE.add(jourBonusAnciennete);

        BigDecimal joursCongesMensuels = totalJoursCongesAnnuels.divide(BigDecimal.valueOf(12),2,RoundingMode.HALF_UP);
        if(employe.getSoldeJoursConge() == null){
            employe.setSoldeJoursConge(BigDecimal.ZERO);
        }

        employe.setSoldeJoursConge(employe.getSoldeJoursConge().add(joursCongesMensuels));

        employeRepo.save(employe);
    }

    public Employe findEmployeById(Long id) {
        return employeRepo.findById(id).orElse(null);
    }

    public List<Employe> findByEntreprise(Entreprise entreprise) {
        return employeRepo.findByEntreprise(entreprise);
    }



}
