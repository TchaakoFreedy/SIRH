package com.fric.sirh.repository;

import com.fric.sirh.model.SoldeConge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoldeCongeRepository extends MongoRepository<SoldeConge, String> {
    List<SoldeConge> findByEmployeeId(String employeeId);
}