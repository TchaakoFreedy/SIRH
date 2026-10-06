package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'SYSTEM_ADMIN')")
public @interface CanManageUsers {
}