// src/main/java/com/fric/sirh/cacao/repository/RemboursementRepository.java

package com.fric.sirh.cacao.repository;

import com.fric.sirh.cacao.model.Remboursement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RemboursementRepository extends MongoRepository<Remboursement, String> {

    List<Remboursement> findByAcheteurIdOrderByDateRemboursementDesc(String acheteurId);

    List<Remboursement> findByReceptionId(String receptionId);

    @Query(value = "{ 'acheteurId': ?0 }", fields = "{ 'montantRembourse': 1 }")
    List<Remboursement> findMontantsByAcheteurId(String acheteurId);

    default BigDecimal sumMontantRembourseByAcheteurId(String acheteurId) {
        return findMontantsByAcheteurId(acheteurId).stream()
                .map(Remboursement::getMontantRembourse)
                .filter(montant -> montant != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}