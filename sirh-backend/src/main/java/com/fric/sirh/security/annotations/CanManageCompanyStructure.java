package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('COMPANY_CREATE', 'COMPANY_UPDATE', 'COMPANY_DELETE', " +
        "'DEPARTMENT_CREATE', 'DEPARTMENT_UPDATE', 'DEPARTMENT_DELETE', " +
        "'POSITION_CREATE', 'POSITION_UPDATE', 'POSITION_DELETE', 'SYSTEM_ADMIN')")
public @interface CanManageCompanyStructure {
}