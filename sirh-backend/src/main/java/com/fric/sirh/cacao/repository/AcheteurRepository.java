// src/main/java/com/fric/sirh/cacao/repository/AcheteurRepository.java

package com.fric.sirh.cacao.repository;

import com.fric.sirh.cacao.model.Acheteur;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcheteurRepository extends MongoRepository<Acheteur, String> {

    Optional<Acheteur> findByEmployeeId(String employeeId);

    List<Acheteur> findByStatut(String statut);

    @Query("{ 'statut': 'ACTIF' }")
    List<Acheteur> findAllActifs();
}