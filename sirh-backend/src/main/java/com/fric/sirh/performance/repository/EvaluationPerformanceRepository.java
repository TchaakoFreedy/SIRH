package com.fric.sirh.performance.repository;

import com.fric.sirh.performance.model.EvaluationPerformance;
import com.fric.sirh.performance.enums.PeriodeEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationPerformanceRepository extends MongoRepository<EvaluationPerformance, String> {

    // ✅ CORRECTION : Utilisation de @Query explicite pour DBRef
    @Query("{ 'employe.$id' : ?0 }")
    Page<EvaluationPerformance> findByEmployeId(String employeId, Pageable pageable);

    @Query("{ 'employe.$id' : ?0 }")
    List<EvaluationPerformance> findByEmployeId(String employeId);

    @Query("{ 'annee' : ?0 }")
    List<EvaluationPerformance> findByAnnee(Integer annee);

    @Query("{ 'employe.$id' : ?0, 'periode' : ?1, 'annee' : ?2 }")
    EvaluationPerformance findByEmployeIdAndPeriodeAndAnnee(String employeId, PeriodeEvaluation periode, Integer annee);

    @Query("{ 'employe.$id' : ?0, 'periode' : ?1, 'annee' : ?2, 'mois' : ?3 }")
    EvaluationPerformance findByEmployeIdAndPeriodeAndAnneeAndMois(String employeId, PeriodeEvaluation periode, Integer annee, Integer mois);

    @Query("{ 'employe.$id' : ?0, 'annee' : ?1 }")
    List<EvaluationPerformance> findByEmployeIdAndAnnee(String employeId, Integer annee);

    @Query("{ 'employe.$id' : ?0, 'periode' : ?1 }")
    List<EvaluationPerformance> findByEmployeIdAndPeriode(String employeId, PeriodeEvaluation periode);

    @Query("{ 'typeEvaluation' : ?0 }")
    Page<EvaluationPerformance> findByTypeEvaluationWithQuery(String typeEvaluation, Pageable pageable);
}