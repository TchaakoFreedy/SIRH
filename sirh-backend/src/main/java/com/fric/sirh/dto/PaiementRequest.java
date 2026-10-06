package com.fric.sirh.dto;

import com.fric.sirh.model.TypePaiement;
import lombok.Data;

@Data
public class PaiementRequest {
    private String employeeId;
    private TypePaiement type;
    private Double montant;
    private String motif;
    private Integer mois;
    private Integer annee;
}