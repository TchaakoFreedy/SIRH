package com.fric.sirh.repository;

import com.fric.sirh.model.Employee;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {

    @Query("{ 'managerId' : ?0 }")
    List<Employee> getTeamByManager(String managerId);

    // ============================================================
    // RECHERCHE PAR MATRICULE INTERNE
    // ============================================================

    Optional<Employee> findByMatriculeInterne(String matriculeInterne);

    boolean existsByMatriculeInterne(String matriculeInterne);

    boolean existsByMatriculeInterneAndIdNot(String matriculeInterne, String id);

    // ============================================================
    // RECHERCHE PAR MATRICULE CNPS
    // ============================================================

    /**
     * Vérifie si un matricule CNPS existe déjà.
     * Retourne Boolean (objet) pour éviter les problèmes d'AOP avec les primitifs.
     */
    @Query("{ 'matricule_CNPS' : ?0 }")
    Boolean existsByMatriculeCNPS(String matriculeCNPS);

    /**
     * Vérifie si un matricule CNPS existe déjà pour un autre employé.
     */
    @Query("{ 'matricule_CNPS' : ?0, 'id' : { $ne: ?1 } }")
    Boolean existsByMatriculeCNPSAndIdNot(String matriculeCNPS, String id);

    /**
     * Recherche un employé par matricule CNPS.
     */
    @Query("{ 'matricule_CNPS' : ?0 }")
    Optional<Employee> findByMatriculeCNPS(String matriculeCNPS);

    // ============================================================
    // RECHERCHE PAR MATRICULE
    // ============================================================

    /**
     * Recherche par matricule interne.
     * Alias vers matriculeInterne.
     */
    @Query("{ 'matriculeInterne' : ?0 }")
    Optional<Employee> findByMatricule(String matricule);

    // ============================================================
    // RECHERCHE PAR NOM / PRENOM
    // ============================================================

    List<Employee> findByNomContainingIgnoreCase(String nom);

    List<Employee> findByPrenomContainingIgnoreCase(String prenom);

    // ============================================================
    // RECHERCHE PAR UTILISATEUR
    // ============================================================

    @Query("{ 'user.$id': ?0 }")
    Optional<Employee> findByUserId(String userId);

    @Query("{ 'user.email' : ?0 }")
    Optional<Employee> findByUserEmail(String email);

    boolean existsByUserId(String userId);

    // ============================================================
    // RECHERCHE PAR DEPARTEMENT
    // ============================================================

    List<Employee> findByDepartementId(String departementId);

    List<Employee> findByDepartementIdIn(List<String> departementIds);

    List<Employee> findByDepartementIdInAndStatut(
            List<String> departementIds,
            String statut
    );

    // ============================================================
    // RECHERCHE PAR POSTE
    // ============================================================

    List<Employee> findByPosteId(String posteId);

    // ============================================================
    // RECHERCHE PAR STATUT
    // ============================================================

    List<Employee> findByStatut(String statut);

    // ============================================================
    // RECHERCHE PAR ENTREPRISE
    // ============================================================

    List<Employee> findByEntrepriseId(String entrepriseId);

    // ============================================================
    // RECHERCHE GLOBALE
    // ============================================================

    @Query("{ $or: [ " +
            "{ 'nom': { $regex: ?0, $options: 'i' } }, " +
            "{ 'prenom': { $regex: ?0, $options: 'i' } }, " +
            "{ 'matriculeInterne': { $regex: ?0, $options: 'i' } }, " +
            "{ 'matricule_CNPS': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Employee> searchEmployees(String query);

    // ============================================================
    // AGREGATIONS FILTREES PAR DEPARTEMENT
    // ============================================================

    long countByDepartementIdIn(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $match: { 'departementId': { $in: ?0 } } }",
            "{ $group: { _id: '$sexe', count: { $sum: 1 } } }"
    })
    List<SexeCount> countBySexeGroupBySexe(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $match: { 'departementId': { $in: ?0 } } }",
            "{ $group: { _id: '$departementId', count: { $sum: 1 } } }"
    })
    List<DepartmentCount> countByDepartementIdGroupByDepartement(
            List<String> departementIds
    );

    @Aggregation(pipeline = {
            "{ $match: { 'departementId': { $in: ?0 }, 'date_embauche': { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: { $month: '$date_embauche' }, count: { $sum: 1 } } }"
    })
    List<MonthlyCount> countByMonthOfDateEmbauche(
            List<String> departementIds,
            LocalDate start,
            LocalDate end
    );

    // ============================================================
    // AGREGATIONS POUR TOUS LES EMPLOYES
    // ============================================================

    @Aggregation(pipeline = {
            "{ $group: { _id: '$sexe', count: { $sum: 1 } } }"
    })
    List<SexeCount> countBySexeGroupBySexeAll();

    @Aggregation(pipeline = {
            "{ $group: { _id: '$departementId', count: { $sum: 1 } } }"
    })
    List<DepartmentCount> countByDepartementIdGroupByDepartementAll();

    @Aggregation(pipeline = {
            "{ $match: { 'date_embauche': { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: { $month: '$date_embauche' }, count: { $sum: 1 } } }"
    })
    List<MonthlyCount> countByMonthOfDateEmbaucheAll(
            LocalDate start,
            LocalDate end
    );

    // ============================================================
    // PROJECTIONS POUR LES AGREGATIONS
    // ============================================================

    class SexeCount {

        @Field("_id")
        private String id;

        private long count;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    class DepartmentCount {

        @Field("_id")
        private String id;

        private long count;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    class MonthlyCount {

        @Field("_id")
        private int id;

        private long count;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}