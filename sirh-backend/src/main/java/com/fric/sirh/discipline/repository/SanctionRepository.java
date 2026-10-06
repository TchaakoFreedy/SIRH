package com.fric.sirh.discipline.repository;

import com.fric.sirh.discipline.model.Sanction;
import com.fric.sirh.discipline.enums.StatutSanction;
import com.fric.sirh.discipline.enums.TypeSanction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SanctionRepository extends MongoRepository<Sanction, String> {

    // Pour un seul employé (convention Spring Data)
    Page<Sanction> findByEmploye_Id(String employeeId, Pageable pageable);
    List<Sanction> findByEmploye_Id(String employeeId);

    // Pour plusieurs employés
    @Query("{ 'employe.$id' : { $in: ?0 } }")
    List<Sanction> findByEmployeIdIn(List<String> employeeIds);

    // Autres méthodes
    Page<Sanction> findByStatut(StatutSanction statut, Pageable pageable);
    Page<Sanction> findByType(TypeSanction type, Pageable pageable);

    @Query("{ 'dateDebut': { $gte: ?0, $lte: ?1 } }")
    Page<Sanction> findByDateDebutBetween(LocalDateTime debut, LocalDateTime fin, Pageable pageable);

    List<Sanction> findByStatutAndDateFinBefore(StatutSanction statut, LocalDateTime date);

    boolean existsByNumero(String numero);
}