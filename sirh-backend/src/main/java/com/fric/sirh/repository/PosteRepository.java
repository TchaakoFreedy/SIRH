package com.fric.sirh.repository;

import com.fric.sirh.model.Poste;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosteRepository extends MongoRepository<Poste, String> {

    Optional<Poste> findByCode(String code);

    boolean existsByCode(String code);

    List<Poste> findByActiveTrue();

    List<Poste> findByActiveFalse();

    // ==========================================
    // ✅ MÉTHODES AVEC LA SYNTAXE CORRECTE
    // ==========================================

    // ✅ Recherche par département ID (champ simple)
    @Query("{ 'departement.$id': ?0 }")
    List<Poste> findByDepartementId(String departementId);

    // ✅ Recherche par liste de département ID
    @Query("{ 'departement.$id': { $in: ?0 } }")
    List<Poste> findByDepartementIdIn(List<String> departementIds);

    // ✅ Recherche par département ID avec ObjectId (alternative)
    @Query("{ 'departement.$id': ObjectId(?0) }")
    List<Poste> findByDepartementIdWithObjectId(String departementId);

    // ✅ Recherche par liste de département ID avec ObjectId
    @Query("{ 'departement.$id': { $in: ?0 } }")
    List<Poste> findByDepartementIdInWithObjectId(List<String> departementIds);

    // ✅ Alternative : utiliser le champ string
    @Query("{ 'departementId': ?0 }")
    List<Poste> findByDepartementIdString(String departementId);

    @Query("{ 'departementId': { $in: ?0 } }")
    List<Poste> findByDepartementIdStringIn(List<String> departementIds);

    // ✅ Avec statut actif
    @Query("{ 'departement.$id': ?0, 'active': true }")
    List<Poste> findByDepartementIdAndActiveTrue(String departementId);

    @Query("{ 'departement.$id': { $in: ?0 }, 'active': true }")
    List<Poste> findByDepartementIdInAndActiveTrue(List<String> departementIds);

    // ✅ Recherche par entreprise (via département)
    @Query("{ 'departement.entrepriseId': ?0 }")
    List<Poste> findByDepartementEntrepriseId(String entrepriseId);

    @Query("{ 'departement.entrepriseId': ?0, 'active': true }")
    List<Poste> findByDepartementEntrepriseIdAndActiveTrue(String entrepriseId);

    List<Poste> findByLibelleContainingIgnoreCase(String libelle);

    @Query("{ $or: [ " +
            "{ 'code': { $regex: ?0, $options: 'i' } }, " +
            "{ 'libelle': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Poste> searchByCodeOrLibelle(String searchTerm);
}