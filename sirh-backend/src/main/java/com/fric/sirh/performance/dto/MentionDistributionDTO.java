package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionDistributionDTO {
    private String mention;
    private Integer count;
    private Double pourcentage;
}