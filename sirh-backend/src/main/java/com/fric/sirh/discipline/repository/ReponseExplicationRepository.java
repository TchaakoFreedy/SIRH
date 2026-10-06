package com.fric.sirh.discipline.repository;

import com.fric.sirh.discipline.model.ReponseExplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReponseExplicationRepository extends MongoRepository<ReponseExplication, String> {
}