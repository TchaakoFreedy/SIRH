package com.fric.sirh.mapper;

import com.fric.sirh.dto.UserDTO;
import com.fric.sirh.model.User;

import java.util.List;

public class UserMapper {

    public static UserDTO toDTO(User user) {

        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .active(user.getActive())
                .employeeId(user.getEmployeeId())

                // ✅ FIX ICI (PAS DE atStartOfDay)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())

                .permissions(List.of())
                .roleName(user.getRoleId())
                .roleLevel(null)
                .build();
    }

    public static List<UserDTO> toDTOList(List<User> users) {
        return users.stream()
                .map(UserMapper::toDTO)
                .toList();
    }
}