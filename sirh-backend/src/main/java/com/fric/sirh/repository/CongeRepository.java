package com.fric.sirh.repository;

import com.fric.sirh.enums.StatutConge;
import com.fric.sirh.enums.TypeConge;
import com.fric.sirh.model.Conge;
import com.fric.sirh.model.Employee;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.List;

public interface CongeRepository extends MongoRepository<Conge, String> {

    // === Méthodes existantes (inchangées) ===
    List<Conge> findByEmployee(Employee employee);
    List<Conge> findByStatut(StatutConge statut);
    List<Conge> findByTypeConge(TypeConge type);
    List<Conge> findByEmployeeAndAnnee(Employee employee, Integer annee);
    List<Conge> findByEmployeeAndStatut(Employee employee, StatutConge statut);
    List<Conge> findByEmployeeAndStatutAndAnnee(Employee employee, StatutConge statut, Integer annee);
    List<Conge> findByEmployeeAndTypeCongeAndStatut(Employee employee, TypeConge typeConge, StatutConge statut);
    List<Conge> findByEmployeeAndTypeCongeAndStatutAndAnnee(Employee employee, TypeConge typeConge, StatutConge statut, Integer annee);

    @Query("{ 'employee': ?0, 'statut': { $in: ?1 } }")
    List<Conge> findByEmployeeAndStatutIn(Employee employee, List<StatutConge> statuts);

    // === Méthodes avec IDs (format DBRef) - version String (existante, peut rester) ===
    @Query("{ 'employee.$id': { $in: ?0 } }")
    List<Conge> findByEmployeeIds(List<String> employeeIds);

    @Query("{ 'employee': { $in: ?0 } }")
    List<Conge> findByEmployeeIdsSimple(List<String> employeeIds);

    @Query("{ 'employee.$id': { $in: ?0 }, 'statut': ?1 }")
    List<Conge> findByEmployeeIdsAndStatut(List<String> employeeIds, StatutConge statut);

    @Query("{ 'employee': { $in: ?0 }, 'statut': ?1 }")
    List<Conge> findByEmployeeIdsSimpleAndStatut(List<String> employeeIds, StatutConge statut);

    @Query("{ 'employee.$id': { $in: ?0 }, 'typeConge': ?1 }")
    List<Conge> findByEmployeeIdsAndTypeConge(List<String> employeeIds, TypeConge typeConge);

    @Query("{ 'employee': { $in: ?0 }, 'typeConge': ?1 }")
    List<Conge> findByEmployeeIdsSimpleAndTypeConge(List<String> employeeIds, TypeConge typeConge);

    // === Méthodes avec ObjectId (recommandées pour DBRef) ===
    @Query("{ 'employee.$id': { $in: ?0 } }")
    List<Conge> findByEmployeeObjectIds(List<ObjectId> employeeIds);

    @Query("{ 'employee.$id': { $in: ?0 }, 'statut': ?1 }")
    List<Conge> findByEmployeeObjectIdsAndStatut(List<ObjectId> employeeIds, StatutConge statut);

    @Query("{ 'employee.$id': { $in: ?0 }, 'statut': ?1, 'annee': ?2 }")
    List<Conge> findByEmployeeObjectIdsAndStatutAndAnnee(List<ObjectId> employeeIds, StatutConge statut, Integer annee);

    // === Méthodes robustes (multi-format) ===
    @Query("{ $or: [ { 'employee.$id': { $in: ?0 } }, { 'employee': { $in: ?0 } } ] }")
    List<Conge> findByEmployeeIdsAny(List<String> employeeIds);

    @Query("{ $or: [ { 'employee.$id': { $in: ?0 } }, { 'employee': { $in: ?0 } } ], 'statut': ?1 }")
    List<Conge> findByEmployeeIdsAnyAndStatut(List<String> employeeIds, StatutConge statut);

    // === Méthodes avec liste d'objets Employee ===
    @Query("{ 'employee': { $in: ?0 } }")
    List<Conge> findByEmployeeList(List<Employee> employees);

    @Query("{ 'employee': { $in: ?0 }, 'statut': ?1 }")
    List<Conge> findByEmployeeListAndStatut(List<Employee> employees, StatutConge statut);

    // === Agrégations avec $lookup (filtrées par département) ===
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'EN_ATTENTE' } }"
    })
    List<Conge> findPendingByDepartments(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'APPROUVE' } }"
    })
    List<Conge> findApprovedByDepartments(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'APPROUVE', 'jourDebut': { $lte: ?1 }, 'jourFin': { $gte: ?1 } } }"
    })
    List<Conge> findApprovedOnDateByDepartments(List<String> departementIds, LocalDate date);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': 'APPROUVE', 'dateValidation': { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: { $month: '$dateValidation' }, count: { $sum: 1 } } }"
    })
    List<MonthlyCount> countApprovedByMonthForDepartments(List<String> departementIds, LocalDate start, LocalDate end);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 } } }"
    })
    List<Conge> findAllByDepartments(List<String> departementIds);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'statut': ?1 } }"
    })
    List<Conge> findAllByDepartmentsAndStatut(List<String> departementIds, String statut);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', let: { empId: '$employee' }, pipeline: [ { $match: { $expr: { $eq: ['$_id', '$$empId'] } } } ], as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 }, 'typeConge': ?1 } }"
    })
    List<Conge> findAllByDepartmentsAndType(List<String> departementIds, String typeConge);

    long countByEmployeeAndStatut(Employee employee, StatutConge statut);

    // === Méthodes "ALL" ===
    @Aggregation(pipeline = {
            "{ $match: { 'statut': 'APPROUVE', 'jourDebut': { $lte: ?0 }, 'jourFin': { $gte: ?0 } } }"
    })
    List<Conge> findApprovedOnDateForAll(LocalDate date);

    @Aggregation(pipeline = {
            "{ $match: { 'statut': 'EN_ATTENTE' } }"
    })
    List<Conge> findPendingAll();

    @Aggregation(pipeline = {
            "{ $match: { 'statut': 'APPROUVE' } }"
    })
    List<Conge> findApprovedAll();

    @Aggregation(pipeline = {
            "{ $match: { 'statut': 'APPROUVE', 'dateValidation': { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: { $month: '$dateValidation' }, count: { $sum: 1 } } }"
    })
    List<MonthlyCount> countApprovedByMonthAll(LocalDate start, LocalDate end);

    // ============================================================
    // CLASSE DE PROJECTION
    // ============================================================
    static class MonthlyCount {
        @Field("_id")
        private int id;
        private long count;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}