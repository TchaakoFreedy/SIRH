package com.fric.sirh.model;

import org.springframework.data.annotation.Id;
import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import lombok.Data;

@Data
@Document(collection = "Performance")

public class Performance {
    @Id
    private String id;
    private String periode;
    private String note;
    private String description;
    private LocalDate createdAt;
    private String createdBy;
    private String updatedBy;
    private LocalDate updatedAt;

    @DBRef
    private Employee employee;

    public Performance(){}
    public Performance(String periode, String note, String description,String createdBy, LocalDate createdAt, String updatedBy, LocalDate updatedAt){
        this.description = description;
        this.note = note;
        this.periode = periode;
        this.createdAt = createdAt;
        this.createdBy =createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
}
