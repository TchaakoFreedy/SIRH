package com.fric.sirh.dto;

import lombok.Data;

@Data
public class PosteRequest {
    private String code;
    private String libelle;
    private String description;
    private String departementId;
}