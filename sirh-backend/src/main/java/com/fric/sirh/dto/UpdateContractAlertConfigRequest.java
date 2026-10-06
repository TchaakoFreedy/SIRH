// src/main/java/com/fric/sirh/dto/UpdateContractAlertConfigRequest.java
package com.fric.sirh.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContractAlertConfigRequest {

    private Boolean enabled;

    @Min(value = 1, message = "Le nombre de jours doit être au moins 1")
    private Integer daysBefore;

    private List<String> emailRecipients;
    private List<String> emailCc;
    private String emailSubject;
    private String emailBodyTemplate;
}