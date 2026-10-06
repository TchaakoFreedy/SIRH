package com.fric.sirh.service;

import com.fric.sirh.dto.CreateContratRequest;
import com.fric.sirh.dto.CreateUserRequest;
import com.fric.sirh.dto.RegisterEmployeeRequest;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeRegistrationService {

    private final UserService userService;
    private final EmployeeService employeeService;
    private final ContratService contratService;

    @Transactional
    public Employee register(RegisterEmployeeRequest request) {

        // sera complété juste après

        return null;
    }

}