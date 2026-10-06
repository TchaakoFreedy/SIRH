package com.fric.sirh.repository;

import com.fric.sirh.model.Contrat;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContratRepository extends MongoRepository<Contrat, String> {

    // === Méthodes existantes ===
    List<Contrat> findByEmployee_Id(String employeeId);
    List<Contrat> findByEmployee_IdAndStatut(String employeeId, String statut);

    // === NOUVELLE MÉTHODE EXPLICITE AVEC DBRef ===
    @Query("{ 'employee.$id' : ?0, 'statut' : ?1 }")
    List<Contrat> findActiveContractsByEmployeeId(String employeeId, String statut);

    List<Contrat> findByStatut(String statut);
    List<Contrat> findByTypeContrat(String typeContrat);
    long countByStatut(String statut);
    boolean existsByEmployee_IdAndStatut(String employeeId, String statut);
    List<Contrat> findTop10ByOrderByCreatedAtDesc();

    // Méthodes pour les contrats expirant
    @Query("{ 'dateFin': { $gte: ?0, $lte: ?1 } }")
    List<Contrat> findContractsExpiringBetween(LocalDate startDate, LocalDate endDate);

    List<Contrat> findByDateFinBetween(LocalDate startDate, LocalDate endDate);

    @Query("{ 'statut': ?0, 'dateFin': { $gte: ?1, $lte: ?2 } }")
    List<Contrat> findByStatutAndDateFinBetween(String statut, LocalDate startDate, LocalDate endDate);

    // === Agrégations avec $lookup (filtrées par département) ===
    long countByEmployee_DepartementIdInAndStatut(List<String> departementIds, String statut);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'ACTIF' } }",
            "{ $group: { _id: '$typeContrat', count: { $sum: 1 } } }"
    })
    List<TypeCount> countActiveByTypeForDepartments(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'ACTIF', 'dateFin': { $gte: ?1, $lte: ?2 } } }"
    })
    List<Contrat> findActiveExpiringBetweenForDepartments(List<String> departementIds, LocalDate start, LocalDate end);

    // === Méthode "ALL" ===
    @Aggregation(pipeline = {
            "{ $match: { 'statut': 'ACTIF' } }",
            "{ $group: { _id: '$typeContrat', count: { $sum: 1 } } }"
    })
    List<TypeCount> countActiveByTypeAll();

    // === Méthodes pour la Direction (avec DBRef) ===
    @Query("{ 'employee.$id' : { $in: ?0 } }")
    List<Contrat> findByEmployeeIdIn(List<ObjectId> employeeIds);

    @Query(value = "{ 'employee.$id' : { $in: ?0 }, 'statut' : ?1 }", count = true)
    long countByEmployeeIdInAndStatut(List<ObjectId> employeeIds, String statut);

    // ============================================================
    // CLASSE DE PROJECTION
    // ============================================================
    static class TypeCount {
        @Field("_id")
        private String id;
        private long count;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}