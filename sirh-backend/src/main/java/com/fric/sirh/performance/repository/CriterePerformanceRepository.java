package com.fric.sirh.performance.repository;

import com.fric.sirh.performance.model.CriterePerformance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriterePerformanceRepository extends MongoRepository<CriterePerformance, String> {

    List<CriterePerformance> findByActifTrue();

    List<CriterePerformance> findByActifTrueAndTypeCritere(String typeCritere);

    // Find global criteria (applicable to all)
    List<CriterePerformance> findByActifTrueAndTypeCritereOrderByOrdreAffichageAsc(String typeCritere);

    // Find selective criteria that include a specific employee
    @Query("{ 'actif': true, 'typeCritere': 'SELECTIVE', 'employeeIds': { $in: [?0] } }")
    List<CriterePerformance> findByActifTrueAndTypeCritereAndEmployeeIdsContaining(String typeCritere, String employeeId);

    // Find criteria by employee IDs list
    @Query("{ 'actif': true, 'typeCritere': 'SELECTIVE', 'employeeIds': { $in: ?0 } }")
    List<CriterePerformance> findByActifTrueAndTypeCritereAndEmployeeIdsIn(String typeCritere, List<String> employeeIds);

    // Find criteria by department IDs
    @Query("{ 'actif': true, 'typeCritere': 'SELECTIVE', 'departementIds': { $in: ?0 } }")
    List<CriterePerformance> findByActifTrueAndTypeCritereAndDepartementIdsIn(String typeCritere, List<String> departementIds);
}