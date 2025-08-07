package com.hades.maalipo.dto;

import com.hades.maalipo.enum1.MethodePaiement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BulletinPaieCreateDto {
    private Long employeId;
    private Long entrepriseId;
    private BigDecimal heuresSup;
    private BigDecimal heuresFerie;
    private BigDecimal heuresNuit;
    private LocalDate datePaiement;
    private MethodePaiement methodePaiement;
    private BigDecimal avancesSurSalaires;
}
