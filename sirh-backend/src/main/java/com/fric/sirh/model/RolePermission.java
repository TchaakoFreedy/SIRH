package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "RolePermission")
public class RolePermission {
    @DBRef
    private Role role;
    private Permission permission;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;
}
