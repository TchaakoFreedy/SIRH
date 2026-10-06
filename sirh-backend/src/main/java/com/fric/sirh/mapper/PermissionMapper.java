package com.fric.sirh.mapper;

import com.fric.sirh.dto.PermissionDto;
import com.fric.sirh.model.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionDto toDto(Permission permission) {
        if (permission == null) {
            return null;
        }

        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .category(permission.getCategory())
                .requiredLevel(permission.getRequiredLevel())
                .active(permission.getActive())
                .createdAt(permission.getCreatedAt())
                .createdBy(permission.getCreatedBy())
                .updatedAt(permission.getUpdatedAt())
                .updatedBy(permission.getUpdatedBy())
                .build();
    }

    public Permission toEntity(PermissionDto dto) {
        if (dto == null) {
            return null;
        }

        return Permission.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .requiredLevel(dto.getRequiredLevel())
                .active(dto.getActive())
                .createdAt(dto.getCreatedAt())
                .createdBy(dto.getCreatedBy())
                .updatedAt(dto.getUpdatedAt())
                .updatedBy(dto.getUpdatedBy())
                .build();
    }
}