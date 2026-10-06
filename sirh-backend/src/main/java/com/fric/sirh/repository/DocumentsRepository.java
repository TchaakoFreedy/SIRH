package com.fric.sirh.repository;

import com.fric.sirh.model.Documents;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentsRepository extends MongoRepository<Documents, String> {

    List<Documents> findByEmployee_Id(String employeeId);
    List<Documents> findByContrat_Id(String contratId);
    List<Documents> findByTypeDocument(String typeDocument);
    List<Documents> findByEmployee_IdAndTypeDocument(String employeeId, String typeDocument);

    // === Agrégation filtrée par département ===
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', localField: 'employee', foreignField: '_id', as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $match: { 'emp.departementId': { $in: ?0 } } }",
            "{ $group: { _id: '$emp._id', types: { $addToSet: '$typeDocument' } } }",
            "{ $match: { 'types': { $not: { $in: ?1 } } } }",
            "{ $count: 'missing' }"
    })
    Long countEmployeesMissingDocumentTypes(List<String> departementIds, List<String> mandatoryTypes);

    // === MÉTHODE "ALL" ===
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'employees', localField: 'employee', foreignField: '_id', as: 'emp' } }",
            "{ $unwind: '$emp' }",
            "{ $group: { _id: '$emp._id', types: { $addToSet: '$typeDocument' } } }",
            "{ $match: { 'types': { $not: { $in: ?0 } } } }",
            "{ $count: 'missing' }"
    })
    Long countEmployeesMissingDocumentTypesAll(List<String> mandatoryTypes);
}