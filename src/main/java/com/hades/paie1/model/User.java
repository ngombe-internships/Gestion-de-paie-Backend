package com.hades.paie1.model;

import com.fasterxml.jackson.annotation.*;
import com.hades.paie1.enum1.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column (nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'EMPLOYE'")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private PasswordResetToken passwordResetToken;



    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    //@JsonManagedReference("user-employe")
    @JsonIdentityReference(alwaysAsId = true)
    private Employe employe;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", unique = true)
    //@JsonBackReference("user-entreprise")
    @JsonIdentityReference(alwaysAsId = true)
    private Entreprise entreprise;


}
