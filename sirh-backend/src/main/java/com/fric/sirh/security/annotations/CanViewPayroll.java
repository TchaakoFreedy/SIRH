package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('PAYSLIP_VIEW', 'PAYSLIP_VIEW_ALL', 'SYSTEM_ADMIN')")
public @interface CanViewPayroll {
}