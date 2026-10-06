package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyAuthority('CONTRACT_CREATE', 'CONTRACT_UPDATE', 'CONTRACT_TERMINATE', 'SYSTEM_ADMIN')")
public @interface CanManageContracts {
}