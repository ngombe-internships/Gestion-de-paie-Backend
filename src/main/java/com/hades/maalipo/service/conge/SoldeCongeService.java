package com.hades.maalipo.service.conge;

import com.hades.maalipo.dto.conge.HistoriqueCongeDto;
import com.hades.maalipo.dto.conge.SoldeCongeDto;
import com.hades.maalipo.enum1.StatutDemandeConge;
import com.hades.maalipo.enum1.TypeConge;
import com.hades.maalipo.model.DemandeConge;
import com.hades.maalipo.model.Employe;
import com.hades.maalipo.model.SoldeHistorique;
import com.hades.maalipo.repository.DemandeCongeRepository;
import com.hades.maalipo.repository.EmployeRepository;
import com.hades.maalipo.repository.SoldeHistoriqueRepository;
import com.hades.maalipo.utils.JourOuvrableCalculatorUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SoldeCongeService {

    private final SoldeHistoriqueRepository historiqueRepository;
    private final EmployeRepository employeRepository;
    private final DemandeCongeRepository demandeCongeRepository;
    private final JourOuvrableCalculatorUtil jourOuvrableCalculator;

    public SoldeCongeService(
            SoldeHistoriqueRepository soldeHistoriqueRepository,
            EmployeRepository employeRepository,
            DemandeCongeRepository demandeCongeRepository,
            JourOuvrableCalculatorUtil jourOuvrableCalculator
    ){
        this.historiqueRepository = soldeHistoriqueRepository;
        this.employeRepository = employeRepository;
        this.demandeCongeRepository = demandeCongeRepository;
        this.jourOuvrableCalculator = jourOuvrableCalculator;
    }

    private static final BigDecimal JOURS_PAR_MOIS_ACQUISITION = new BigDecimal("1.5"); // 1,5 jour par mois
    private static final int BASE_ANNUELLE_CAMEROUN = 18; // 18 jours ouvrables par an
    private static final int PERIODE_ESSAI_MOIS = 6; // 6 mois sans congés
    private static final int SEUIL_ANCIENNETE_BONUS = 5; // Bonus à partir de 5 ans
    private static final int BONUS_PAR_ANNEE_SUPPLEMENTAIRE = 2; // +2 jours PAR ANNÉE au-delà de 5 ans


    public BigDecimal calculerSoldeAcquisAnnuelCourant(Employe employe) {
        LocalDate dateEmbauche = employe.getDateEmbauche();
        LocalDate maintenant = LocalDate.now();

        // Période d'essai : pas de congés
        if (estEnPeriodeEssai(employe)) {
            return BigDecimal.ZERO;
        }

        // Vérifier si l'employé a fini sa période d'essai avant cette année
        int anneeEnCours = maintenant.getYear();
        LocalDate debutAcquisition = dateEmbauche.plusMonths(PERIODE_ESSAI_MOIS);
        LocalDate debutAnnee = LocalDate.of(anneeEnCours, 1, 1);

        // Si la période d'essai n'est pas terminée au début de cette année
        if (debutAcquisition.isAfter(debutAnnee)) {
            // Si la période d'essai se termine cette année ET qu'elle est déjà finie
            if (debutAcquisition.getYear() == anneeEnCours && !debutAcquisition.isAfter(maintenant)) {
                // Retourner le droit annuel complet (même si acquis en cours d'année)
                return calculerDroitAnnuelSelonAnciennete(employe);
            } else if (debutAcquisition.isAfter(maintenant)) {
                // Période d'essai pas encore terminée
                return BigDecimal.ZERO;
            }
        }

        // ✅ CORRECTION PRINCIPALE : Retourner le droit annuel COMPLET
        return calculerDroitAnnuelSelonAnciennete(employe);
    }

    private BigDecimal calculerDroitAnnuelSelonAnciennete(Employe employe) {
        long anneesAnciennete = ChronoUnit.YEARS.between(employe.getDateEmbauche(), LocalDate.now());

        int droitAnnuel = BASE_ANNUELLE_CAMEROUN; // 18 jours de base

        // Bonus d'ancienneté : +2 jours PAR ANNÉE au-delà de 5 ans
        if (anneesAnciennete > SEUIL_ANCIENNETE_BONUS) {
            long anneesBonus = anneesAnciennete - SEUIL_ANCIENNETE_BONUS;
            int bonusTotal = (int) (anneesBonus * BONUS_PAR_ANNEE_SUPPLEMENTAIRE);
            droitAnnuel += bonusTotal;
        }

        return new BigDecimal(droitAnnuel);
    }

    // MÉTHODE PRINCIPALE : Remplace calculerSoldeAcquisTotal
    public BigDecimal calculerSoldeAcquisTotal(Employe employe) {
        return calculerSoldeAcquisAnnuelCourant(employe);
    }

    // Calcul des droits selon l'ancienneté (pour information/debug)
    public int calculerDroitsSelonAnciennete(Employe employe) {
        long anneesAnciennete = ChronoUnit.YEARS.between(employe.getDateEmbauche(), LocalDate.now());

        int droitAnnuel = BASE_ANNUELLE_CAMEROUN; // 18 jours de base

        if (anneesAnciennete > SEUIL_ANCIENNETE_BONUS) {
            long anneesBonus = anneesAnciennete - SEUIL_ANCIENNETE_BONUS;
            int bonusTotal = (int) (anneesBonus * BONUS_PAR_ANNEE_SUPPLEMENTAIRE);
            droitAnnuel += bonusTotal;
        }

        return droitAnnuel;
    }

    public boolean estEnPeriodeEssai(Employe employe) {
        long moisDepuisEmbauche = ChronoUnit.MONTHS.between(employe.getDateEmbauche(), LocalDate.now());
        return moisDepuisEmbauche < PERIODE_ESSAI_MOIS;
    }

    public BigDecimal calculerJoursReellementPris(Employe employe) {
        int anneeEnCours = LocalDate.now().getYear();
        LocalDate debutAnnee = LocalDate.of(anneeEnCours, 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeEnCours, 12, 31);

        List<DemandeConge> demandesApprouvees = demandeCongeRepository
                .findByEmployeAndStatutAndDateBetween(
                        employe,
                        StatutDemandeConge.APPROUVEE,
                        debutAnnee,
                        finAnnee
                );

        BigDecimal totalJoursPris = BigDecimal.ZERO;

        for (DemandeConge demande : demandesApprouvees) {
            if (demande.getTypeConge() == TypeConge.CONGE_PAYE) {
                long jours = jourOuvrableCalculator.calculateWorkingDays(
                        demande.getDateDebut(),
                        demande.getDateFin(),
                        employe
                );
                totalJoursPris = totalJoursPris.add(BigDecimal.valueOf(jours));
            }
        }

        return totalJoursPris;
    }

    public BigDecimal calculerSoldeDisponibleCorrect(Employe employe) {
        BigDecimal soldeAcquis = calculerSoldeAcquisTotal(employe);
        BigDecimal joursPris = calculerJoursReellementPris(employe);

        BigDecimal soldeDisponible = soldeAcquis.subtract(joursPris);

        // S'assurer qu'on ne retourne jamais un solde négatif
        return soldeDisponible.max(BigDecimal.ZERO);
    }


    public BigDecimal calculerSoldeDisponible(Employe employe) {
        return calculerSoldeDisponibleCorrect(employe);
    }

    public boolean aSoldeSuffisant(Employe employe, long joursDemanges) {
        BigDecimal soldeDisponible = calculerSoldeDisponible(employe);
        return soldeDisponible.compareTo(new BigDecimal(joursDemanges)) >= 0;
    }

    @Transactional
    public void mettreAJourSoldeApresApprobation(Employe employe, long joursPris) {
        BigDecimal soldeActuel = employe.getSoldeJoursConge();
        BigDecimal nouveauSolde = soldeActuel.subtract(BigDecimal.valueOf(joursPris));

        if (nouveauSolde.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Solde insuffisant pour décompter " + joursPris + " jours");
        }

        employe.setSoldeJoursConge(nouveauSolde);
        employeRepository.save(employe);

        // Historique
        SoldeHistorique historique = new SoldeHistorique();
        historique.setEmploye(employe);
        historique.setDateCalcul(LocalDate.now());
        historique.setSoldeAcquis(soldeActuel);
        historique.setSoldePris(BigDecimal.valueOf(joursPris));
        historiqueRepository.save(historique);
    }

    @Transactional
    public void restaurerSoldeApresAnnulation(Employe employe, long joursARestaurer) {
        BigDecimal soldeActuel = employe.getSoldeJoursConge();
        BigDecimal nouveauSolde = soldeActuel.add(BigDecimal.valueOf(joursARestaurer));

        employe.setSoldeJoursConge(nouveauSolde);
        employeRepository.save(employe);

        // Historique de restauration
        SoldeHistorique historique = new SoldeHistorique();
        historique.setEmploye(employe);
        historique.setDateCalcul(LocalDate.now());
        historique.setSoldeAcquis(nouveauSolde);
        historique.setSoldePris(BigDecimal.valueOf(-joursARestaurer));
        historiqueRepository.save(historique);
    }

    // Méthode utilitaire pour debug/admin avec calcul correct
    public String getDetailCalculSolde(Employe employe) {
        LocalDate dateEmbauche = employe.getDateEmbauche();
        LocalDate maintenant = LocalDate.now();
        long anneesAnciennete = ChronoUnit.YEARS.between(dateEmbauche, maintenant);

        StringBuilder detail = new StringBuilder();
        detail.append("=== CALCUL CONGÉS CAMEROUN 2025 (CORRIGÉ) ===\n");
        detail.append("Employé : ").append(employe.getPrenom()).append(" ").append(employe.getNom()).append("\n");
        detail.append("Date d'embauche : ").append(dateEmbauche).append("\n");
        detail.append("Ancienneté : ").append(anneesAnciennete).append(" ans\n");

        // Calcul du droit annuel selon l'ancienneté
        int droitAnnuel = BASE_ANNUELLE_CAMEROUN;
        detail.append("Base légale : ").append(BASE_ANNUELLE_CAMEROUN).append(" jours\n");

        if (anneesAnciennete > SEUIL_ANCIENNETE_BONUS) {
            long anneesBonus = anneesAnciennete - SEUIL_ANCIENNETE_BONUS;
            int bonusTotal = (int) (anneesBonus * BONUS_PAR_ANNEE_SUPPLEMENTAIRE);
            droitAnnuel += bonusTotal;
            detail.append("Bonus ancienneté : +").append(bonusTotal).append(" jours (")
                    .append(anneesBonus).append(" années × 2)\n");
        }

        detail.append("Droit annuel total : ").append(droitAnnuel).append(" jours\n");

        // Calcul pour l'année en cours
        int anneeEnCours = maintenant.getYear();
        LocalDate debutAnnee = LocalDate.of(anneeEnCours, 1, 1);
        LocalDate debutAcquisition = dateEmbauche.plusMonths(PERIODE_ESSAI_MOIS);
        LocalDate dateDebutCalcul = debutAcquisition.isAfter(debutAnnee) ? debutAcquisition : debutAnnee;

        long moisTravailles = ChronoUnit.MONTHS.between(dateDebutCalcul, maintenant);

        detail.append("=== CALCUL ANNÉE ").append(anneeEnCours).append(" ===\n");
        detail.append("Début calcul : ").append(dateDebutCalcul).append("\n");
        detail.append("Mois travaillés : ").append(moisTravailles).append("/12\n");
        detail.append("Prorata : ").append(droitAnnuel).append(" × ").append(moisTravailles).append("/12 = ");

        BigDecimal soldeCalcule = calculerSoldeAcquisTotal(employe);
        detail.append(soldeCalcule).append(" jours\n");

        return detail.toString();
    }

    public List<HistoriqueCongeDto> getHistoriqueConges(Long employeId) {
        int anneeEnCours = LocalDate.now().getYear();
        LocalDate debutAnnee = LocalDate.of(anneeEnCours, 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeEnCours, 12, 31);

        // Récupérer toutes les demandes de congé de l'employé pour cette année
        List<DemandeConge> demandes = demandeCongeRepository.findByEmployeIdAndDateBetween(
                employeId, debutAnnee, finAnnee
        );

        return demandes.stream()
                .map(this::convertToHistoriqueDto)
                .sorted((d1, d2) -> d2.getDateDebut().compareTo(d1.getDateDebut())) // Plus récent d'abord
                .collect(Collectors.toList());
    }

    //Convertit une demande de congé en DTO d'historique
    private HistoriqueCongeDto convertToHistoriqueDto(DemandeConge demande) {
        BigDecimal joursOuvrables = BigDecimal.ZERO;

        // Calculer les jours ouvrables seulement pour les congés approuvés
        if (demande.getStatut() == StatutDemandeConge.APPROUVEE) {
            long jours = calculateWorkingDays(demande.getDateDebut(), demande.getDateFin(), demande.getEmploye());
            joursOuvrables = BigDecimal.valueOf(jours);
        }

        return new HistoriqueCongeDto(
                demande.getId(),
                demande.getTypeConge().getLibelle(),
                demande.getDateDebut(),
                demande.getDateFin(),
                joursOuvrables,
                demande.getDateApprobationRejet(),
                demande.getStatut().name(),
                demande.getMotifRejet()
        );
    }

    private long calculateWorkingDays(LocalDate dateDebut, LocalDate dateFin, Employe employe) {
        return jourOuvrableCalculator.calculateWorkingDays(dateDebut, dateFin, employe);
    }

    public SoldeCongeDto construireSoldeDto(Employe employe) {
        BigDecimal soldeAcquis = calculerSoldeAcquisTotal(employe);
        BigDecimal soldePris = calculerJoursReellementPris(employe);
        BigDecimal soldeDisponible = soldeAcquis.subtract(soldePris).max(BigDecimal.ZERO);

        return new SoldeCongeDto(
                employe.getId(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getMatricule(),
                soldeAcquis,
                soldeDisponible,
                soldePris,
                employe.getDateEmbauche(),
                estEnPeriodeEssai(employe)
        );
    }

}