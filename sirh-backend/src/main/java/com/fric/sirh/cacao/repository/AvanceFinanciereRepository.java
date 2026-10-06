// src/main/java/com/fric/sirh/cacao/repository/AvanceFinanciereRepository.java

package com.fric.sirh.cacao.repository;

import com.fric.sirh.cacao.model.AvanceFinanciere;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AvanceFinanciereRepository extends MongoRepository<AvanceFinanciere, String> {

    List<AvanceFinanciere> findByAcheteurIdOrderByDateAvanceDesc(String acheteurId);

    @Query(value = "{ 'acheteurId': ?0 }", fields = "{ 'montant': 1 }")
    List<AvanceFinanciere> findMontantsByAcheteurId(String acheteurId);

    default BigDecimal sumMontantByAcheteurId(String acheteurId) {
        return findMontantsByAcheteurId(acheteurId).stream()
                .map(AvanceFinanciere::getMontant)
                .filter(montant -> montant != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}