package com.fric.sirh.discipline.repository;

import com.fric.sirh.discipline.model.DemandeExplication;
import com.fric.sirh.discipline.enums.StatutDemandeExplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DemandeExplicationRepository extends MongoRepository<DemandeExplication, String> {

    // Avec Pageable
    Page<DemandeExplication> findByEmployeConcerneId(String employeeId, Pageable pageable);

    // Sans Pageable (pour récupérer toutes les demandes d'un employé)
    @Query("{ 'employeConcerne.$id' : ?0 }")
    List<DemandeExplication> findByEmployeConcerneId(String employeeId);

    Page<DemandeExplication> findByAuteurId(String auteurId, Pageable pageable);

    Page<DemandeExplication> findByEntrepriseId(String entrepriseId, Pageable pageable);

    Page<DemandeExplication> findByDepartementId(String departementId, Pageable pageable);

    Page<DemandeExplication> findByStatut(StatutDemandeExplication statut, Pageable pageable);

    @Query("{ 'dateCreation': { $gte: ?0, $lte: ?1 } }")
    Page<DemandeExplication> findByDateCreationBetween(LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    @Query("{ 'employeConcerne.$id': ?0, 'statut': ?1 }")
    List<DemandeExplication> findByEmployeConcerneIdAndStatut(String employeeId, StatutDemandeExplication statut);

    @Query("{ $or: [ { 'employeConcerne.$id': ?0 }, { 'auteur.$id': ?0 } ] }")
    Page<DemandeExplication> findByEmployeConcerneIdOrAuteurId(String userId, Pageable pageable);

    boolean existsByNumero(String numero);

    List<DemandeExplication> findByStatutAndDateLimiteReponseBefore(StatutDemandeExplication statut, LocalDateTime date);
}