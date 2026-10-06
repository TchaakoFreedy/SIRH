package com.fric.sirh.repository;

import com.fric.sirh.model.Paiement;
import com.fric.sirh.model.TypePaiement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementRepository extends MongoRepository<Paiement, String> {

    List<Paiement> findByEmployeeIdOrderByDatePaiementDesc(String employeeId);

    List<Paiement> findByEmployeeIdAndMoisAndAnnee(String employeeId, Integer mois, Integer annee);

    List<Paiement> findByEmployeeIdAndMoisAndAnneeAndType(
            String employeeId,
            Integer mois,
            Integer annee,
            TypePaiement type
    );

    List<Paiement> findByMoisAndAnnee(Integer mois, Integer annee);

    List<Paiement> findByDatePaiementBetween(LocalDateTime start, LocalDateTime end);

    List<Paiement> findAllByOrderByDatePaiementDesc();
}