package com.fric.sirh.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.fric.sirh.model.Absence;

@Repository
public interface AbsenceRepository extends MongoRepository<Absence, String> {
    // Cette méthode est indispensable pour que le CongeService puisse filtrer
    List<Absence> findByEmployeeId(String employeeId);
}