package com.fric.sirh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
public class Permission {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String category;

    private Integer requiredLevel;

    @Builder.Default
    private Boolean active = true;

    private LocalDate createdAt;
    private String createdBy;
    private LocalDate updatedAt;
    private String updatedBy;
}