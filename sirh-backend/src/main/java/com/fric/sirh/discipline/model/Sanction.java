package com.fric.sirh.discipline.model;

import com.fric.sirh.discipline.enums.StatutSanction;
import com.fric.sirh.discipline.enums.TypeSanction;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sanctions")
public class Sanction {

    @Id
    private String id;

    private String numero; // SAN-2026-0001

    @DBRef
    private Employee employe;

    @DBRef
    private DemandeExplication demandeExplication;

    private TypeSanction type;

    private String motif;

    private String description;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    private Integer duree; // en jours

    private StatutSanction statut;

    @DBRef
    private User creePar;

    @Builder.Default
    private List<HistoriqueDiscipline> historique = new ArrayList<>();

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}