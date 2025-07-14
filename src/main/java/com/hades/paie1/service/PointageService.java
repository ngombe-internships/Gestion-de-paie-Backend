//package com.hades.paie1.service;
//
//import com.hades.paie1.enum1.PointageType;
//import com.hades.paie1.exception.RessourceNotFoundException;
//import com.hades.paie1.model.Employe;
//import com.hades.paie1.model.Pointage;
//import com.hades.paie1.repository.EmployeRepository;
//import com.hades.paie1.repository.PointageRepository;
//import jakarta.transaction.Transactional;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//public class PointageService {
//
//    private PointageRepository pointageRepository;
//    private JourFerieService jourferieService;
//    private EmployeRepository employeRepository;
//
//    public PointageService (PointageRepository pointageRepository ,
//                            JourFerieService jourferieService,
//                            EmployeRepository employeRepository){
//        this.jourferieService = jourferieService;
//        this.pointageRepository = pointageRepository;
//        this.employeRepository = employeRepository;
//    }
//
//    @Transactional
//    public Pointage savePointage(Pointage pointage){
//        //verifie existance employe
//        Employe employe = employeRepository.findById(pointage.getEmploye().getId())
//                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouve avec l'ID :" +pointage.getEmploye().getId()));
//        pointage.setEmploye(employe);
//
////        if (pointage.getTypePointage() == PointageType.ENTREE) {
////                 employe.setStatut("EN_SERVICE");
////             } else if (pointage.getTypePointage() == PointageType.SORTIE) {
////                 employe.setStatut("HORS_SERVICE");
////            }
////            employeRepository.save(employe);
//
//        // Par exemple: récupérer le dernier pointage de l'employé et vérifier le type.
//        // Si le dernier pointage était un "ENTREE" et le nouveau est "ENTREE", c'est une erreur.
//        // Si le dernier pointage était un "SORTIE" ou aucun, et le nouveau est "SORTIE", c'est une erreur.
//
//        // Exemple simplifié de validation (vous devrez l'affiner):
////        List<Pointage> pointagesDuJour = pointageRepository.findByEmployeIdAndDatePointageBetweenOrderByDatePointageAscHeurePointageAsc(
////                employe.getId(), pointage.getDatePointage(), pointage.getDatePointage());
////
////        if (pointage.getTypePointage() == TypePointage.ENTREE) {
////            if (!pointagesDuJour.isEmpty() && pointagesDuJour.get(pointagesDuJour.size() - 1).getTypePointage() == TypePointage.ENTREE) {
////                throw new IllegalArgumentException("L'employé a déjà un pointage d'entrée non suivi d'une sortie pour cette journée.");
////            }
////        } else if (pointage.getTypePointage() == TypePointage.SORTIE) {
////            if (pointagesDuJour.isEmpty() || pointagesDuJour.get(pointagesDuJour.size() - 1).getTypePointage() == TypePointage.SORTIE) {
////                throw new IllegalArgumentException("L'employé doit avoir un pointage d'entrée avant un pointage de sortie.");
////            }
////        }
//
//        if (pointage.getLagitude() != null && pointage.getLongitude() != null && employe.getEntreprise() != null){
//
//            if(employe.getEntreprise().getLatitudeEntreprise() != null &&
//                    employe.getEntreprise().getLongitudeEntreprise() != null &&
//                    employe.getEntreprise().getRadiusToleranceMeters() != null){
//
//                double distance ;
//            }
//        }
//        return pointageRepository.save(pointage);
//    }
//
//    public List<Pointage> getPointagesByEmployeAndDate(Long employeId, LocalDate startDate, LocalDate endDate){
//        Employe employe = employeRepository.findById(employeId)
//                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouve avec l'ID "+ employeId));
//        return pointageRepository.findByEmployeAndDatePointageBetweenOrderByDatePointageAsc(employe, startDate,endDate);
//    }
//
//    //calcul les heure en categorise
//    public Map<String, Double> calculateCategorizedHours(Long employeId,LocalDate startDate, LocalDate endDate ){
//
//        Employe employe = employeRepository.findById(employeId)
//                .orElseThrow(()-> new RessourceNotFoundException("Employe non trouve avec Id " +employeId));
//
//        BigDecimal heuresContractuellesHebdomadaires = employe.getHeuresContractuellesHebdomadaires();
//        Integer joursOuvrablesContractuelsHebdomadaires = employe.getJoursOuvrablesContractuelsHebdomadaires();
//
//
//        if(heuresContractuellesHebdomadaires == null || joursOuvrablesContractuelsHebdomadaires == null){
//            throw  new IllegalArgumentException("Les heures contractuelles ou les jours ouvrables hebdomadaire de l'employes ne sont pas definis ou sont nuls.");
//        }
//
//        double heuresNormalesParJour = heuresContractuellesHebdomadaires.doubleValue()/joursOuvrablesContractuelsHebdomadaires;
//
//        List<Pointage> rawpointages = getPointagesByEmployeAndDate(employeId, startDate, endDate);
//
//        double totalHeuresNormales = 0.0;
//        double totalHeureSup = 0.0;
//        double totalHeuresFeries = 0.0;
//
//        Map<LocalDate, List<Pointage>> pointagesParJour = rawpointages.stream()
//                .collect(Collectors.groupingBy(Pointage::getDatePointage));
//
//
//        for (Map.Entry<LocalDate, List<Pointage>> entry: pointagesParJour.entrySet()) {
//
//            LocalDate date = entry.getKey();
//            List<Pointage> dailyPunches = entry.getValue();
//
//            dailyPunches.sort(Comparator.comparing(Pointage::getHeurePointage));
//
//            LocalTime lastPushIn = null;
//            double heuresTravaillesJour = 0.0;
//
//            for(Pointage punch : dailyPunches) {
//                if(punch.getTypePointage() == PointageType.ENTREE){
//                    lastPushIn = punch.getHeurePointage();
//                } else if(punch.getTypePointage() == PointageType.SORTIE){
//                    if(lastPushIn != null) {
//                        Duration duration = Duration.between(lastPushIn, punch.getHeurePointage());
//                         if(!duration.isNegative()){
//                             heuresTravaillesJour += duration.toMinutes() / 60.0;
//                         }
//                         lastPushIn = null;
//                    }
//                }
//        }
//            //verifie si cesu un jour ferier
//            boolean isJourFerie = jourferieService.isJourFerie(date);
//
//            if (isJourFerie) {
//                totalHeuresFeries += heuresTravaillesJour;
//            } else {
//                //pour implementer la difference entre heures normal et heur sup en fonction du contrat
//                if (heuresTravaillesJour <= heuresNormalesParJour){
//                    totalHeuresNormales += heuresTravaillesJour;
//                } else {
//                    totalHeuresNormales += heuresNormalesParJour;
//                    totalHeureSup += (heuresTravaillesJour - heuresNormalesParJour);
//                }
//            }
//        }
//        return Map.of(
//                    "heuresNormales",totalHeuresNormales,
//                    "heureSup",totalHeureSup,
//                    "heureFeries",totalHeuresFeries
//        );
//
//
//    }
//
//
//}
//
//
//
////package com.hades.paie1.service;
////
////import com.hades.paie1.exception.RessourceNotFoundException; // Assurez-vous d'avoir cette exception
////import com.hades.paie1.model.Employe;
////import com.hades.paie1.model.Pointage;
////import com.hades.paie1.model.TypePointage;
////import com.hades.paie1.repository.EmployeRepository;
////import com.hades.paie1.repository.PointageRepository;
////// ... Autres imports si nécessaires (JourFerieService)
////
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.time.Duration;
////import java.time.LocalDate;
////import java.time.LocalDateTime;
////import java.time.LocalTime;
////import java.util.List;
////import java.util.Map;
////import java.util.TreeMap;
////import java.util.stream.Collectors;
////
////@Service
////public class PointageService {
////
////    private final PointageRepository pointageRepository;
////    private final EmployeRepository employeRepository;
////    private final JourFerieService jourFerieService; // Assurez-vous que ce service existe et est injecté
////
////    public PointageService(PointageRepository pointageRepository, EmployeRepository employeRepository, JourFerieService jourFerieService) {
////        this.pointageRepository = pointageRepository;
////        this.employeRepository = employeRepository;
////        this.jourFerieService = jourFerieService;
////    }
////
////    // --- Méthodes du Service ---
////
////    /**
////     * Enregistre un nouveau pointage dans la base de données.
////     * Applique des validations métier avant l'enregistrement.
////     *
////     * @param pointage L'objet Pointage à enregistrer.
////     * @return Le pointage enregistré.
////     * @throws RessourceNotFoundException Si l'employé spécifié n'existe pas.
////     * @throws IllegalArgumentException Si le pointage est invalide (ex: pointage de sortie sans pointage d'entrée correspondant).
////     */
////    @Transactional
////    public Pointage savePointage(Pointage pointage) {
////        Employe employe = employeRepository.findById(pointage.getEmploye().getId())
////                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + pointage.getEmploye().getId()));
////
////        pointage.setEmploye(employe); // S'assurer que l'entité Employe est bien gérée par JPA
////
////        // Valider la logique du pointage (ex: pas deux ENTRÉE consécutives, pas de SORTIE sans ENTRÉE)
////        // ... Votre logique de validation détaillée ici ...
////        // Par exemple: récupérer le dernier pointage de l'employé et vérifier le type.
////        // Si le dernier pointage était un "ENTREE" et le nouveau est "ENTREE", c'est une erreur.
////        // Si le dernier pointage était un "SORTIE" ou aucun, et le nouveau est "SORTIE", c'est une erreur.
////
////        // Exemple simplifié de validation (vous devrez l'affiner):
////        List<Pointage> pointagesDuJour = pointageRepository.findByEmployeIdAndDatePointageBetweenOrderByDatePointageAscHeurePointageAsc(
////                employe.getId(), pointage.getDatePointage(), pointage.getDatePointage());
////
////        if (pointage.getTypePointage() == TypePointage.ENTREE) {
////            if (!pointagesDuJour.isEmpty() && pointagesDuJour.get(pointagesDuJour.size() - 1).getTypePointage() == TypePointage.ENTREE) {
////                throw new IllegalArgumentException("L'employé a déjà un pointage d'entrée non suivi d'une sortie pour cette journée.");
////            }
////        } else if (pointage.getTypePointage() == TypePointage.SORTIE) {
////            if (pointagesDuJour.isEmpty() || pointagesDuJour.get(pointagesDuJour.size() - 1).getTypePointage() == TypePointage.SORTIE) {
////                throw new IllegalArgumentException("L'employé doit avoir un pointage d'entrée avant un pointage de sortie.");
////            }
////        }
////
////
////        return pointageRepository.save(pointage);
////    }
////
////    /**
////     * Récupère tous les pointages pour un employé donné sur une période spécifique.
////     *
////     * @param employeId L'ID de l'employé.
////     * @param startDate La date de début de la période.
////     * @param endDate La date de fin de la période.
////     * @return Une liste de pointages.
////     * @throws RessourceNotFoundException Si l'employé n'est pas trouvé.
////     */
////    public List<Pointage> getPointagesByEmployeAndDate(Long employeId, LocalDate startDate, LocalDate endDate) {
////        // Vérifiez si l'employé existe avant de chercher les pointages
////        employeRepository.findById(employeId)
////                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + employeId));
////
////        return pointageRepository.findByEmployeIdAndDatePointageBetweenOrderByDatePointageAscHeurePointageAsc(employeId, startDate, endDate);
////    }
////
////    /**
////     * Calcule les heures catégorisées (normales, supplémentaires, fériées) pour un employé sur une période donnée.
////     *
////     * @param employeId L'ID de l'employé.
////     * @param startDate La date de début de la période de calcul.
////     * @param endDate La date de fin de la période de calcul.
////     * @return Une Map contenant les heures catégorisées.
////     * @throws RessourceNotFoundException Si l'employé n'est pas trouvé.
////     * @throws IllegalArgumentException Si les heures contractuelles de l'employé ne sont pas définies.
////     */
////    public Map<String, Double> calculateCategorizedHours(Long employeId, LocalDate startDate, LocalDate endDate) {
////        Employe employe = employeRepository.findById(employeId)
////                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + employeId));
////
////        if (employe.getHeuresContractuellesHebdomadaires() == null || employe.getHeuresContractuellesHebdomadaires() <= 0) {
////            throw new IllegalArgumentException("Les heures contractuelles hebdomadaires de l'employé doivent être définies pour calculer les heures.");
////        }
////        if (employe.getJoursOuvrablesContractuelsHebdomadaires() == null || employe.getJoursOuvrablesContractuelsHebdomadaires() <= 0) {
////            throw new IllegalArgumentException("Les jours ouvrables contractuels hebdomadaires de l'employé doivent être définis pour calculer les heures.");
////        }
////
////        double heuresNormalesParJour = employe.getHeuresContractuellesHebdomadaires() / employe.getJoursOuvrablesContractuelsHebdomadaires();
////
////        List<Pointage> pointages = getPointagesByEmployeAndDate(employeId, startDate, endDate);
////
////        // Regrouper les pointages par date
////        Map<LocalDate, List<Pointage>> pointagesParDate = pointages.stream()
////                .collect(Collectors.groupingBy(Pointage::getDatePointage));
////
////        double totalHeuresNormales = 0.0;
////        double totalHeuresSup = 0.0;
////        double totalHeuresFeries = 0.0;
////
////        for (Map.Entry<LocalDate, List<Pointage>> entry : pointagesParDate.entrySet()) {
////            LocalDate date = entry.getKey();
////            List<Pointage> dailyPointages = entry.getValue();
////
////            // Assurez-vous que les pointages sont triés par heure (déjà fait par le repository, mais bonne pratique de vérifier)
////            dailyPointages.sort((p1, p2) -> p1.getHeurePointage().compareTo(p2.getHeurePointage()));
////
////            double heuresTravailleesCeJour = 0.0;
////
////            // Parcourir les paires ENTREE/SORTIE
////            for (int i = 0; i < dailyPointages.size() - 1; i++) {
////                Pointage p1 = dailyPointages.get(i);
////                Pointage p2 = dailyPointages.get(i + 1);
////
////                if (p1.getTypePointage() == TypePointage.ENTREE && p2.getTypePointage() == TypePointage.SORTIE) {
////                    LocalDateTime startDateTime = LocalDateTime.of(p1.getDatePointage(), p1.getHeurePointage());
////                    LocalDateTime endDateTime = LocalDateTime.of(p2.getDatePointage(), p2.getHeurePointage());
////
////                    Duration duration = Duration.between(startDateTime, endDateTime);
////                    heuresTravailleesCeJour += (double) duration.toMinutes() / 60.0;
////                    i++; // Passer au pointage suivant après avoir traité la paire
////                }
////            }
////
////            // Catégorisation des heures
////            if (jourFerieService.isJourFerie(date)) {
////                totalHeuresFeries += heuresTravailleesCeJour;
////            } else {
////                if (heuresTravailleesCeJour > heuresNormalesParJour) {
////                    totalHeuresNormales += heuresNormalesParJour;
////                    totalHeuresSup += (heuresTravailleesCeJour - heuresNormalesParJour);
////                } else {
////                    totalHeuresNormales += heuresTravailleesCeJour;
////                }
////            }
////        }
////
////        Map<String, Double> categorizedHours = new TreeMap<>();
////        categorizedHours.put("heuresNormales", totalHeuresNormales);
////        categorizedHours.put("heuresSup", totalHeuresSup);
////        categorizedHours.put("heuresFeries", totalHeuresFeries);
////
////        return categorizedHours;
////    }
////}
//
//
//// Dans com.hades.paie1.service/PointageService.java
//
//// ... imports existants ...
////// Importez la classe Pointage.LocalisationStatus
////import com.hades.paie1.model.Pointage.LocalisationStatus;
////
////public class PointageService {
////
////    // ... constructeur et autres méthodes ...
////
////    @Transactional
////    public Pointage savePointage(Pointage pointage) {
////        Employe employe = employeRepository.findById(pointage.getEmploye().getId())
////                .orElseThrow(() -> new RessourceNotFoundException("Employé non trouvé avec l'ID: " + pointage.getEmploye().getId()));
////
////        pointage.setEmploye(employe); // S'assurer que l'entité Employe est bien gérée par JPA
////
////        // --- NOUVELLE LOGIQUE DE VÉRIFICATION DE LOCALISATION ---
////        if (pointage.getLatitude() != null && pointage.getLongitude() != null && employe.getEntreprise() != null) {
////            // Assurez-vous que l'entité Entreprise est correctement chargée avec toutes ses infos
////            // Elle devrait l'être si vous avez utilisé @JsonManagedReference/@JsonBackReference
////            // Mais si elle n'est pas complètement chargée (LAZY), vous devrez la recharger ou vous assurer qu'elle est JOINED
////            // Ou que les champs de localisation entreprise sont directement sur l'employé si c'est OneToOne
////            // Pour être sûr, rechargez l'entreprise complète si nécessaire:
////            // Entreprise entreprise = entrepriseRepository.findById(employe.getEntreprise().getId()).orElse(null);
////
////            // Pour l'exemple, supposons que employe.getEntreprise() est pleinement accessible
////            if (employe.getEntreprise().getLatitudeEntreprise() != null &&
////                    employe.getEntreprise().getLongitudeEntreprise() != null &&
////                    employe.getEntreprise().getRadiusToleranceMeters() != null) {
////
////                double distance = calculateDistance(
////                        pointage.getLatitude(),
////                        pointage.getLongitude(),
////                        employe.getEntreprise().getLatitudeEntreprise(),
////                        employe.getEntreprise().getLongitudeEntreprise()
////                );
////
////                if (distance <= employe.getEntreprise().getRadiusToleranceMeters()) {
////                    pointage.setLocalisationStatus(LocalisationStatus.VALIDE_SUR_SITE);
////                } else {
////                    pointage.setLocalisationStatus(LocalisationStatus.HORS_SITE);
////                }
////            } else {
////                pointage.setLocalisationStatus(LocalisationStatus.NON_VERIFIE); // Données d'entreprise manquantes
////            }
////        } else {
////            pointage.setLocalisationStatus(LocalisationStatus.NON_VERIFIE); // Coordonnées du pointage manquantes
////        }
////        // --- FIN NOUVELLE LOGIQUE ---
////
////
////        // ... Votre logique de validation existante pour ENTREE/SORTIE ...
////        // ... (Laissez cette partie intacte) ...
////
////
////        return pointageRepository.save(pointage);
////    }
////
////    // --- Fonctions utilitaires de calcul de distance (à ajouter dans PointageService ou une classe utilitaire) ---
////    /**
////     * Calcule la distance en mètres entre deux points GPS en utilisant la formule de Haversine.
////     *
////     * @param lat1 Latitude du premier point.
////     * @param lon1 Longitude du premier point.
////     * @param lat2 Latitude du deuxième point.
////     * @param lon2 Longitude du deuxième point.
////     * @return La distance en mètres.
////     */
////    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
////        final int R = 6371000; // Rayon de la Terre en mètres
////
////        double latDistance = Math.toRadians(lat2 - lat1);
////        double lonDistance = Math.toRadians(lon2 - lon1);
////
////        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
////                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
////                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
////        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
////
////        return R * c; // Distance en mètres
////    }
////}