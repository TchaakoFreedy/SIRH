package com.fric.sirh.repository;

import com.fric.sirh.model.Entreprise;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EntrepriseRepository extends MongoRepository<Entreprise, String> {
}