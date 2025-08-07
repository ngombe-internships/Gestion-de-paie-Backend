package com.hades.maalipo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hades.maalipo.enum1.TypeAvantageNature;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data

@NoArgsConstructor
@ToString(exclude = "employe")
@EqualsAndHashCode(exclude = "employe")

public class EmployeAvantageNature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference("employe-avantages")
    private Employe employe;

    @Enumerated(EnumType.STRING)
    private TypeAvantageNature typeAvantage;

    private boolean actif = true;
}
