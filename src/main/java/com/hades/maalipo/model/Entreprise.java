package com.hades.maalipo.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "entreprise")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String nom;

    @Column(nullable = false)
    private String adresseEntreprise;

    @Column(name = "email")
    private String emailEntreprise;

    @Column(name = "telephone")
    private String telephoneEntreprise;


    @Column(unique = true)
    private String numeroSiret;

    @Column(nullable = false)
    private LocalDate dateCreation;

    @Column(name = "logo")
    @Basic(fetch = FetchType.LAZY)
    private String logoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreationSysteme;

    @LastModifiedDate
    @Column
    private LocalDateTime dateDerniereMiseAJour;




    @Column(name = "standard_heures_hebdomadaires", precision = 5, scale = 2)
    private BigDecimal standardHeuresHebdomadaires;

    @Column(name = "standard_jours_ouvrables_hebdomadaires")
    private Integer standardJoursOuvrablesHebdomadaires;


    @OneToOne(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   // @JsonManagedReference("user-entreprise")
    @JsonIdentityReference(alwaysAsId = true)
    private  User employeurPrincipal;


    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private List <Employe> employes = new ArrayList<>();


    @Column(name = "template_bulletin_path")
    private String templateBulletinPath;

    @ManyToMany
    @JoinTable(
            name = "entreprise_element_paie",
            joinColumns = @JoinColumn(name = "entreprise_id"),
            inverseJoinColumns = @JoinColumn(name = "element_paie_id")
    )
    private Set<ElementPaie> elementPaieActifs;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_bulletin_template_id", unique = true) // Assurez-vous que cette colonne n'existe pas déjà
    //@JsonManagedReference("entreprise-templates")
    private BulletinTemplate defaultBulletinTemplate;



    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BulletinTemplate> bulletinTemplates = new ArrayList<>();



}
