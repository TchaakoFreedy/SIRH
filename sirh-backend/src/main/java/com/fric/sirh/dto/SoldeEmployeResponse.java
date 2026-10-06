package com.fric.sirh.dto;

import lombok.Data;

@Data
public class SoldeEmployeResponse {

    private String employeeId;
    private String employeeNom;
    private String employeePrenom;
    private String employeeMatricule;
    private String employeePoste;
    private String employeeDepartement;
    private String employeeTelephone;

    private Double salaireMensuel;

    private Integer mois;
    private Integer annee;

    private Double totalAvances;
    private Double totalRetenues;
    private Double totalPaye;

    private Double montantNetAPayer;
    private Double montantRestant;

    private boolean salaireConfigure;
    private boolean soldeDisponible;
}