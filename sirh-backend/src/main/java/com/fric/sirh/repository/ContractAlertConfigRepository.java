package com.fric.sirh.repository;

import com.fric.sirh.model.ContractAlertConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractAlertConfigRepository extends MongoRepository<ContractAlertConfig, String> {
    // Pas besoin de findByCompanyId
}