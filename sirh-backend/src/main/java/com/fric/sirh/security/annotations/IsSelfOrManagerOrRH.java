package com.fric.sirh.security.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("@securityService.isCurrentUser(#userId) or @securityService.isManagerOf(#userId) or hasRole('RH')")
public @interface IsSelfOrManagerOrRH {
    String userId();
}