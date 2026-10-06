package com.fric.sirh.dto;

import com.fric.sirh.model.Employee;
import lombok.Data;

@Data
public class RegisterEmployeeRequest {

    // informations du compte utilisateur
    private CreateUserRequest user;

    // informations RH
    private Employee employee;

    // contrat (optionnel)
    private CreateContratRequest contrat;

}