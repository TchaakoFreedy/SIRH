package com.fric.sirh.mapper;

import com.fric.sirh.dto.RoleDto;
import com.fric.sirh.model.Role;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class RoleMapper {

    public RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .hierarchyLevel(role.getHierarchyLevel())
                .permissionIds(role.getPermissionIds() != null ?
                        new ArrayList<>(role.getPermissionIds()) : new ArrayList<>())
                .visibilityScope(role.getVisibilityScope())
                .active(role.getActive())
                .createdAt(role.getCreatedAt())
                .createdBy(role.getCreatedBy())
                .updatedAt(role.getUpdatedAt())
                .updatedBy(role.getUpdatedBy())
                .build();
    }

    public Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }

        return Role.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .hierarchyLevel(dto.getHierarchyLevel())
                .permissionIds(dto.getPermissionIds() != null ?
                        new ArrayList<>(dto.getPermissionIds()) : new ArrayList<>())
                .visibilityScope(dto.getVisibilityScope())
                .active(dto.getActive())
                .createdAt(dto.getCreatedAt())
                .createdBy(dto.getCreatedBy())
                .updatedAt(dto.getUpdatedAt())
                .updatedBy(dto.getUpdatedBy())
                .build();
    }
}