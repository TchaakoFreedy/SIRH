// com.fric.sirh.repository.PayrollRepository
package com.fric.sirh.repository;

import com.fric.sirh.model.Payroll;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends MongoRepository<Payroll, String> {
    List<Payroll> findByEmployeeId(String employeeId);
    List<Payroll> findByEmployeeMatricule(String matricule);
    Optional<Payroll> findByEmployeeMatriculeAndMonthAndYear(String matricule, int month, int year);
    // autres méthodes utiles
}