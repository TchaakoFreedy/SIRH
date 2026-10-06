package com.fric.sirh.service;

import com.fric.sirh.model.ConfigurationConge;

import java.util.List;
import java.util.Optional;

public interface ConfigurationCongeService {
    ConfigurationConge create(ConfigurationConge config);
    ConfigurationConge update(String id, ConfigurationConge config);
    void delete(String id);
    Optional<ConfigurationConge> getById(String id);
    List<ConfigurationConge> getAll();
    ConfigurationConge getGlobalConfiguration();
    ConfigurationConge getConfigurationForEmployee(String employeeId);
    ConfigurationConge getConfigurationForEmployeeAndYear(String employeeId, Integer annee);

    // ✅ NOUVELLE MÉTHODE : Récupère ou crée une configuration individuelle
    ConfigurationConge getOrCreateIndividualConfiguration(String employeeId);
}