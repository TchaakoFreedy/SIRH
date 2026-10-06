package com.fric.sirh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.fric.sirh.model.typeSanction;

@Repository
public interface TypeSanctionRepository extends MongoRepository<typeSanction, String> {

}