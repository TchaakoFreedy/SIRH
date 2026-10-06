package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {
    private String description;
    private String category;
    private Integer requiredLevel;
    private Boolean active;
}