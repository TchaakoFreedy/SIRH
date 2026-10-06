package com.fric.sirh.repository;

import com.fric.sirh.model.Performance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceRepository extends MongoRepository<Performance, String> {
    List<Performance> findByEmployeeId(String employeeId);
}