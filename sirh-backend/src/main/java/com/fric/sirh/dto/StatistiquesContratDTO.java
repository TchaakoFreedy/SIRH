// src/main/java/com/fric/sirh/dto/StatistiquesContratDTO.java
package com.fric.sirh.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StatistiquesContratDTO {
    private long totalContrats;
    private long contratsActifs;
    private long contratsExpires;
    private long contratsEnAttente;
    private long contratsArchives;
    private long contratsARenouveler;
    private Map<String, Long> parType;
    private Map<String, Long> parStatut;
    private double tauxActif;
}