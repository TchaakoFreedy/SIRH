package com.fric.sirh.repository;

import com.fric.sirh.model.BulletinPaie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepository extends MongoRepository<BulletinPaie, String> {
    List<BulletinPaie> findByEmployeeId(String employeeId);
    Optional<BulletinPaie> findByEmployeeIdAndMonthAndYear(String employeeId, int month, int year);
    boolean existsByEmployeeIdAndMonthAndYear(String employeeId, int month, int year);
    List<BulletinPaie> findByPeriod(String period);
    List<BulletinPaie> findByEmployeeMatricule(String employeeMatricule);
    Optional<BulletinPaie> findByEmployeeMatriculeAndMonthAndYear(String matricule, int month, int year);
}