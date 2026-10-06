// src/main/java/com/fric/sirh/model/ContractAlertConfig.java
package com.fric.sirh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contract_alert_configs")
public class ContractAlertConfig {

    @Id
    private String id; // sera "global" ou un ID fixe

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int daysBefore = 14;

    private List<String> emailRecipients;

    private List<String> emailCc;

    private String emailSubject;

    private String emailBodyTemplate;

    private String updatedBy;

    private LocalDateTime updatedAt;
}