package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('COMPANY_VIEW', 'DEPARTMENT_VIEW', 'POSITION_VIEW', 'SYSTEM_ADMIN')")
public @interface CanViewCompanyStructure {
}