package com.fric.sirh.repository;

import com.fric.sirh.model.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {
    Optional<Permission> findByName(String name);
    boolean existsByName(String name);
    List<Permission> findByIdIn(List<String> ids);
    List<Permission> findByCategory(String category);
    List<Permission> findByActiveTrue();
}