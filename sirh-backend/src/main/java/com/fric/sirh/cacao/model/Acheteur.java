// src/main/java/com/fric/sirh/cacao/model/Acheteur.java

package com.fric.sirh.cacao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "acheteurs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acheteur {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId;

    private String zoneCollecte;

    private String statut;

    private String createdBy;

    private String updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}