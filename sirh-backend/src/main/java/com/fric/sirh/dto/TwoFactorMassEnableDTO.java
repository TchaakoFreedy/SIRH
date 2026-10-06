package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorMassEnableDTO {
    private int activatedCount;
    private Map<String, String> userSecrets;
    private Map<String, Set<String>> userBackupCodes;
}