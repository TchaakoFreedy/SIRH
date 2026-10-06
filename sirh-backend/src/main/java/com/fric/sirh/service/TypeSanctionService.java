package com.fric.sirh.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fric.sirh.model.typeSanction;
import com.fric.sirh.repository.TypeSanctionRepository;

@Service
public class TypeSanctionService {

    private final TypeSanctionRepository repository;

    public TypeSanctionService(TypeSanctionRepository repository) {
        this.repository = repository;
    }

    public List<typeSanction> getAll() {
        return repository.findAll();
    }

    public typeSanction getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Type de sanction introuvable"));
    }

    public typeSanction create(typeSanction type) {

        type.setCreatedAt(LocalDate.now());

        return repository.save(type);
    }

    public typeSanction update(String id, typeSanction type) {

        typeSanction existing = getById(id);

        existing.setLibelle(type.getLibelle());
        existing.setDescription(type.getDescription());

        existing.setUpdatedBy(type.getUpdatedBy());
        existing.setUpdatedAt(LocalDate.now());

        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}