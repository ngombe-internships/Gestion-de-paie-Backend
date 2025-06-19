package com.hades.paie1.service;

import com.hades.paie1.dto.EmployeCreateDto;
import com.hades.paie1.dto.EmployeResponseDto;
import com.hades.paie1.exception.RessourceNotFoundException;
import com.hades.paie1.model.Employe;
import com.hades.paie1.model.User;
import com.hades.paie1.repository.EmployeRepository;
import com.hades.paie1.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeService {

    private EmployeRepository employeRepo;
    private UserRepository userRepository;
    public EmployeService (EmployeRepository employeRepo, UserRepository userRepository){
        this.employeRepo = employeRepo;
        this.userRepository = userRepository;
    }

    //methode pour convertir employeCreateDto en entite Employe
    public Employe convertToEntity(EmployeCreateDto dto) {
        Employe employe = new Employe();

        employe.setMatricule(dto.getMatricule());
        employe.setNom(dto.getNom());
        employe.setPrenom(dto.getPrenom()); // Correction: dto.getPrenom()
        employe.setNumeroCnps(dto.getNumeroCnps());
        employe.setNiu(dto.getNiu());
        employe.setTelephone(dto.getTelephone());
        employe.setEmail(dto.getEmail());
        employe.setAdresse(dto.getAdresse());
        employe.setDateEmbauche(dto.getDateEmbauche());
        employe.setPoste(dto.getPoste());
        employe.setService(dto.getService());
        employe.setClassificationProfessionnelle(dto.getClassificationProfessionnelle());
        employe.setCategorieEnum(dto.getCategorie());
        employe.setEchelonEnum(dto.getEchelon());
        employe.setTypeContratEnum(dto.getTypeContratEnum());
        employe.setDateNaissance(dto.getDateNaissance());
        employe.setCivilite(dto.getCivilite());
        return employe;
    }

    // Methode pour convertir Entite Employe en Employe Respose
    public EmployeResponseDto convertToDto(Employe employe) {
        EmployeResponseDto dto = new EmployeResponseDto();
        dto.setId(employe.getId());
        dto.setMatricule(employe.getMatricule());
        dto.setNom(employe.getNom());
        dto.setPrenom(employe.getPrenom());
        dto.setNumeroCnps(employe.getNumeroCnps());
        dto.setNiu(employe.getNiu());
        dto.setTelephone(employe.getTelephone());
        dto.setEmail(employe.getEmail());
        dto.setAdresse(employe.getAdresse());
        dto.setDateEmbauche(employe.getDateEmbauche());
        dto.setPoste(employe.getPoste());
        dto.setService(employe.getService());
        dto.setClassificationProfessionnelle(employe.getClassificationProfessionnelle());
        dto.setCategorie(employe.getCategorieEnum());
        dto.setEchelon(employe.getEchelonEnum());
        dto.setTypeContratEnum(employe.getTypeContratEnum());
        dto.setCivilite(employe.getCivilite());
        dto.setDateNaissance(employe.getDateNaissance());
        return dto;
    }

    @Transactional
    public EmployeResponseDto createEmploye (EmployeCreateDto employeDto){
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
       Employe savedEmploye = employeRepo.save(employe);

       return convertToDto(savedEmploye);
    }

    @Transactional
    public  EmployeResponseDto updateEmploye(EmployeCreateDto employeDto , Long id){
        Employe existingEmploye = employeRepo.findById(id).
                orElseThrow(() -> new RessourceNotFoundException("Employe non trouve avec ID :" +id));

        if ( !existingEmploye.getMatricule().equals(employeDto.getMatricule()) && employeRepo.existsByMatricule(employeDto.getMatricule())){
            throw new RessourceNotFoundException ("Matricule deja utilise " + employeDto.getMatricule());
        }
        if(employeDto.getNumeroCnps() != null && employeRepo.existsByNumeroCnps(employeDto.getNumeroCnps())){
            throw new RessourceNotFoundException("Numero CNPS deja utilise " +employeDto.getNumeroCnps());
        }
        if(employeDto.getNiu() !=null && employeRepo.existsByNiu(employeDto.getNiu())){
            throw new RessourceNotFoundException("NIU deja utilise" +employeDto.getNiu());
        }
        if(employeDto.getEmail() != null && employeRepo.existsByEmail(employeDto.getEmail())){
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
        existingEmploye.setCivilite(employeDto.getCivilite());


        Employe savedEmploye = employeRepo.save(existingEmploye);

        return convertToDto(savedEmploye);
    }



    public List<EmployeResponseDto> getAllEmploye() {
        return employeRepo.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional <EmployeResponseDto> getEmployeById(Long id){
        return employeRepo.findById(id)
                .map(this::convertToDto);    }


    public  void deleteEmploye (Long id){
        if (!employeRepo.existsById(id)){
            throw new RessourceNotFoundException("Employe non trouve avec ID " +id);
        }
        employeRepo.deleteById(id);
    }

    public  List<EmployeResponseDto> searchEmployes(String searchTerm){
        List<Employe> employes= employeRepo.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String lowerCaseSearchTerm = "%" + searchTerm.trim().toLowerCase() +"%";

                predicates.add(cb.like(cb.lower(root.get("nom")),lowerCaseSearchTerm));
                predicates.add(cb.like(cb.lower(root.get("prenom")),lowerCaseSearchTerm));
                predicates.add(cb.like(cb.lower(root.get("matricule")), lowerCaseSearchTerm));

            }

            if (!predicates.isEmpty()){
                return cb.or(predicates.toArray(new  Predicate[0]));
            } else {
                  return  cb.conjunction();
            }

        });
        return employes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public boolean  isEmployerOfCurrentUser (Long employeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = authentication.getName();
       Optional <User> authenticatedUser = userRepository.findByUsername(username);

       if (authenticatedUser.isEmpty()) {
           return false;
       }

       User user = authenticatedUser.get();

       return user.getEmploye() != null && user.getEmploye().getId().equals(employeId);
    }
}
