// src/main/java/com/fric/sirh/cacao/repository/ReceptionCacaoRepository.java

package com.fric.sirh.cacao.repository;

import com.fric.sirh.cacao.model.ReceptionCacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReceptionCacaoRepository extends MongoRepository<ReceptionCacao, String> {

    List<ReceptionCacao> findByAcheteurIdOrderByDateReceptionDesc(String acheteurId);

    @Query(value = "{ 'acheteurId': ?0 }", fields = "{ 'valeurLivree': 1 }")
    List<ReceptionCacao> findValeursByAcheteurId(String acheteurId);

    default BigDecimal sumValeurLivreeByAcheteurId(String acheteurId) {
        return findValeursByAcheteurId(acheteurId).stream()
                .map(ReceptionCacao::getValeurLivree)
                .filter(valeur -> valeur != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Query(value = "{ 'acheteurId': ?0, 'remboursementId': { $exists: false } }")
    List<ReceptionCacao> findNonRembourseesByAcheteurId(String acheteurId);
}