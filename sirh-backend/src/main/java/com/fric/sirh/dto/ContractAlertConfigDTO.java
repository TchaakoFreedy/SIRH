// src/main/java/com/fric/sirh/dto/ContractAlertConfigDTO.java
package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractAlertConfigDTO {
    private String id;
    private boolean enabled;
    private int daysBefore;
    private List<String> emailRecipients;
    private List<String> emailCc;
    private String emailSubject;
    private String emailBodyTemplate;
    private String updatedBy;
    private String updatedAt;
}