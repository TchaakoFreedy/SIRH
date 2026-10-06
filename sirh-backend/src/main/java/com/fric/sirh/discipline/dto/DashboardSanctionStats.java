package com.fric.sirh.discipline.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DashboardSanctionStats {
    private long total;
    private long actives;
    private long levees;
    private long annulees;
    private Map<String, Long> parType;
    private Map<String, Long> parStatut;
}