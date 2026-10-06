package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'DIRECTION', 'TOP_MANAGER', 'RH')")
public @interface IsEmployeeOrAbove {
}