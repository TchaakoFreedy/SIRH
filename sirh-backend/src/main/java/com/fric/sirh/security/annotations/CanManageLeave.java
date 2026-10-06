package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('LEAVE_APPROVE', 'LEAVE_REJECT', 'SYSTEM_ADMIN')")
public @interface CanManageLeave {
}