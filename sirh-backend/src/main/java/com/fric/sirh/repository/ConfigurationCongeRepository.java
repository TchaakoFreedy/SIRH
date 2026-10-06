package com.fric.sirh.repository;

import com.fric.sirh.model.ConfigurationConge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigurationCongeRepository extends MongoRepository<ConfigurationConge, String> {

    // Global
    Optional<ConfigurationConge> findByTypeAndAnneeIsNull(String type);
    Optional<ConfigurationConge> findByTypeAndAnnee(String type, Integer annee);

    // Individuelle
    Optional<ConfigurationConge> findByTypeAndEmployeeId(String type, String employeeId);
    Optional<ConfigurationConge> findByTypeAndEmployeeIdAndAnnee(String type, String employeeId, Integer annee);

    // Genre
    Optional<ConfigurationConge> findByTypeAndGenreAndAnneeIsNull(String type, String genre);
    Optional<ConfigurationConge> findByTypeAndGenreAndAnnee(String type, String genre, Integer annee);

    // Autres
    List<ConfigurationConge> findByType(String type);
}