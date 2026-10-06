package com.fric.sirh.security;

/**
 * Centralisation des noms de rôles et permissions utilisés dans Spring Security.
 *
 * IMPORTANT :
 * - Les permissions doivent matcher EXACTEMENT les documents MongoDB (Permission.name).
 * - Les rôles MongoDB sont stockés sans préfixe (ex: RH, MANAGER, EMPLOYEE).
 * - Spring Security recevra un rôle avec préfixe: ROLE_ + role.getName().
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    // ===================== ROLES (Spring Authorities) =====================
    public static final String ROLE_RH = "ROLE_RH";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";

    // ===================== PERMISSIONS =====================
    // USER
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_VIEW_ALL = "USER_VIEW_ALL";
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";

    // EMPLOYEE
    public static final String EMPLOYEE_CREATE = "EMPLOYEE_CREATE";
    public static final String EMPLOYEE_VIEW_ALL = "EMPLOYEE_VIEW_ALL";
    public static final String EMPLOYEE_VIEW = "EMPLOYEE_VIEW";
    public static final String EMPLOYEE_UPDATE = "EMPLOYEE_UPDATE";

    // CONTRACT
    public static final String CONTRACT_CREATE = "CONTRACT_CREATE";
    public static final String CONTRACT_VIEW = "CONTRACT_VIEW";

    // DOCUMENTS
    public static final String DOC_UPLOAD = "DOC_UPLOAD";
    public static final String DOC_VIEW_ALL = "DOC_VIEW_ALL";

    // DOCUMENTS (optionnel)
    public static final String DOC_VIEW = "DOC_VIEW";
}

