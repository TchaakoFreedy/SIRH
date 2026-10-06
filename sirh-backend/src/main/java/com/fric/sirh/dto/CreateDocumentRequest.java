package com.fric.sirh.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CreateDocumentRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String typeDocument;

    @NotEmpty(message = "Vous devez fournir au moins une image/url pour ce document")
    private List<String> imageUrls;
}