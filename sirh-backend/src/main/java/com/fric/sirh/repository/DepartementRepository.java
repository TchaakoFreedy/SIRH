package com.fric.sirh.repository;

import com.fric.sirh.model.Departement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartementRepository extends MongoRepository<Departement, String> {

    // ✅ Récupérer tous les départements d'une entreprise
    List<Departement> findByEntrepriseId(String entrepriseId);

    // ✅ Récupérer les départements d'une entreprise par statut
    List<Departement> findByEntrepriseIdAndStatut(String entrepriseId, String statut);

    // ✅ Récupérer les départements par statut
    List<Departement> findByStatut(String statut);

    // ✅ Récupérer un département par nom
    Optional<Departement> findByName(String name);

    // ✅ Récupérer les départements d'une entreprise par nom (recherche)
    List<Departement> findByEntrepriseIdAndNameContainingIgnoreCase(String entrepriseId, String name);

    // ✅ Vérifier l'existence d'un département dans une entreprise
    boolean existsByEntrepriseIdAndId(String entrepriseId, String departementId);

    // ✅ Compter les départements d'une entreprise
    long countByEntrepriseId(String entrepriseId);

    // ✅ Recherche de départements par terme
    @Query("{ $or: [ " +
            "{ 'name': { $regex: ?0, $options: 'i' } }, " +
            "{ 'entrepriseId': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Departement> searchDepartements(String query);

    // ✅ Récupérer les départements par liste d'IDs
    List<Departement> findByIdIn(List<String> ids);
}