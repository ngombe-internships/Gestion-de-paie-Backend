//package com.hades.paie1.model;
//
//
//import com.hades.paie1.enum1.LocalisationStatus;
//import com.hades.paie1.enum1.PointageType;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//
//@Entity
//@Table(name= "pointages")
//@Data
//@AllArgsConstructor
//@Builder
//@NoArgsConstructor
//public class Pointage {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "employe_id", nullable = false)
//    private Employe employe;
//
//    @Column(name = "date_pointage", nullable = false)
//    private LocalDate datePointage;
//
//    @Column(name = "heure_pointage", nullable = false)
//    private LocalTime heurePointage;
//
//    private Double lagitude;
//    private Double longitude;
//    @Enumerated(EnumType.STRING)
//    private LocalisationStatus localisationStatus;
//
//    @Enumerated(EnumType.STRING)
//    private PointageType typePointage;
//}
