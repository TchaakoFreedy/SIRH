package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('SANCTION_CREATE', 'SANCTION_MANAGE', 'SYSTEM_ADMIN')")
public @interface CanManageSanctions {
}