package com.fric.sirh.repository;

import com.fric.sirh.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ==========================================
    // ✅ RECHERCHE PAR EMAIL
    // ==========================================

    /**
     * Recherche un utilisateur par son email
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un utilisateur existe avec cet email
     */
    boolean existsByEmail(String email);

    // ==========================================
    // ✅ RECHERCHE PAR RÔLE
    // ==========================================

    /**
     * Récupère tous les utilisateurs ayant un rôle spécifique
     */
    List<User> findByRoleId(String roleId);

    /**
     * Récupère tous les utilisateurs ayant un rôle spécifique et actifs
     */
    List<User> findByRoleIdAndActiveTrue(String roleId);

    /**
     * Récupère tous les utilisateurs ayant un rôle spécifique et inactifs
     */
    List<User> findByRoleIdAndActiveFalse(String roleId);

    /**
     * Récupère tous les utilisateurs ayant un rôle spécifique et non verrouillés
     */
    List<User> findByRoleIdAndLockedFalse(String roleId);

    // ==========================================
    // ✅ RECHERCHE PAR EMPLOYEE ID
    // ==========================================

    /**
     * Recherche un utilisateur par son employeeId
     */
    Optional<User> findByEmployeeId(String employeeId);

    /**
     * Vérifie si un utilisateur existe avec cet employeeId
     */
    boolean existsByEmployeeId(String employeeId);

    // ==========================================
    // ✅ RECHERCHE PAR STATUT
    // ==========================================

    /**
     * Récupère tous les utilisateurs actifs
     */
    List<User> findByActiveTrue();

    /**
     * Récupère tous les utilisateurs inactifs
     */
    List<User> findByActiveFalse();

    /**
     * Récupère tous les utilisateurs verrouillés
     */
    List<User> findByLockedTrue();

    /**
     * Récupère tous les utilisateurs non verrouillés
     */
    List<User> findByLockedFalse();

    // ==========================================
    // ✅ RECHERCHE PAR NOM
    // ==========================================

    /**
     * Récupère les utilisateurs par prénom (insensible à la casse)
     */
    List<User> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Récupère les utilisateurs par nom (insensible à la casse)
     */
    List<User> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Récupère les utilisateurs par prénom ou nom (insensible à la casse)
     */
    @Query("{ $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<User> findByFirstNameOrLastNameContainingIgnoreCase(String searchTerm);

    // ==========================================
    // ✅ RECHERCHE AVANCÉE
    // ==========================================

    /**
     * Recherche des utilisateurs par nom, prénom ou email (insensible à la casse)
     */
    @Query("{ $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'email': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<User> searchUsers(String query);

    /**
     * Recherche des utilisateurs actifs par nom, prénom ou email
     */
    @Query("{ $and: [ " +
            "{ $or: [ " +
            "  { 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'email': { $regex: ?0, $options: 'i' } } " +
            "] }, " +
            "{ 'active': true } " +
            "] }")
    List<User> searchActiveUsers(String query);

    // ==========================================
    // ✅ RECHERCHE PAR CRÉATEUR / DATE
    // ==========================================

    /**
     * Récupère les utilisateurs créés par un utilisateur spécifique
     */
    List<User> findByCreatedBy(String createdBy);

    /**
     * Récupère les utilisateurs créés entre deux dates
     */
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Récupère les utilisateurs créés après une date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Récupère les utilisateurs créés avant une date
     */
    List<User> findByCreatedAtBefore(LocalDateTime date);

    // ==========================================
    // ✅ RECHERCHE PAR LISTES
    // ==========================================

    /**
     * Récupère les utilisateurs par liste de rôles
     */
    List<User> findByRoleIdIn(List<String> roleIds);

    /**
     * Récupère les utilisateurs par liste d'IDs
     */
    List<User> findByIdIn(List<String> ids);

    // ==========================================
    // ✅ COMPTES / STATISTIQUES
    // ==========================================

    /**
     * Compte les utilisateurs actifs
     */
    long countByActiveTrue();

    /**
     * Compte les utilisateurs inactifs
     */
    long countByActiveFalse();

    /**
     * Compte les utilisateurs verrouillés
     */
    long countByLockedTrue();

    /**
     * Compte les utilisateurs par rôle
     */
    long countByRoleId(String roleId);

    /**
     * Compte les utilisateurs actifs par rôle
     */
    long countByRoleIdAndActiveTrue(String roleId);

    // ==========================================
    // ✅ EXISTENCE
    // ==========================================

    /**
     * Vérifie si un utilisateur existe avec un prénom et un nom
     */
    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Vérifie si un utilisateur existe avec un roleId
     */
    boolean existsByRoleId(String roleId);

    // ==========================================
    // ✅ SUPPRESSION
    // ==========================================

    /**
     * Supprime tous les utilisateurs avec un rôle spécifique
     */
    void deleteByRoleId(String roleId);

    /**
     * Supprime tous les utilisateurs inactifs
     */
    void deleteByActiveFalse();

    // ==========================================
    // ✅ 2FA - RECHERCHES SPÉCIFIQUES
    // ==========================================

    /**
     * Récupère tous les utilisateurs qui ont le 2FA activé
     */
    List<User> findByTwoFactorEnabledTrue();

    /**
     * Récupère tous les utilisateurs qui n'ont pas le 2FA activé
     */
    List<User> findByTwoFactorEnabledFalse();

    /**
     * Récupère les utilisateurs avec 2FA activé par rôle
     */
    List<User> findByRoleIdAndTwoFactorEnabledTrue(String roleId);

    /**
     * Récupère les utilisateurs avec 2FA désactivé par rôle
     */
    List<User> findByRoleIdAndTwoFactorEnabledFalse(String roleId);

    /**
     * Récupère les utilisateurs actifs avec 2FA activé
     */
    List<User> findByActiveTrueAndTwoFactorEnabledTrue();

    /**
     * Récupère les utilisateurs actifs avec 2FA désactivé
     */
    List<User> findByActiveTrueAndTwoFactorEnabledFalse();

    /**
     * Récupère les utilisateurs avec 2FA activé et verrouillés
     */
    List<User> findByTwoFactorEnabledTrueAndLockedTrue();

    /**
     * Compte les utilisateurs avec 2FA activé
     */
    long countByTwoFactorEnabledTrue();

    /**
     * Compte les utilisateurs avec 2FA désactivé
     */
    long countByTwoFactorEnabledFalse();

    /**
     * Compte les utilisateurs avec 2FA activé par rôle
     */
    long countByRoleIdAndTwoFactorEnabledTrue(String roleId);

    /**
     * Récupère les utilisateurs avec 2FA activé créés après une date
     */
    List<User> findByTwoFactorEnabledTrueAndCreatedAtAfter(LocalDateTime date);

    /**
     * Récupère les utilisateurs dont le 2FA a été activé par un utilisateur spécifique
     */
    List<User> findByTwoFactorEnabledBy(String enabledBy);

    /**
     * Récupère les utilisateurs dont le 2FA a été désactivé par un utilisateur spécifique
     */
    List<User> findByTwoFactorDisabledBy(String disabledBy);

    /**
     * Récupère les utilisateurs avec 2FA activé et un secret défini
     */
    @Query("{ 'twoFactorEnabled': true, 'twoFactorSecret': { $ne: null } }")
    List<User> findEnabledUsersWithSecret();

    /**
     * Récupère les utilisateurs avec 2FA activé mais non vérifiés
     */
    List<User> findByTwoFactorEnabledTrueAndTwoFactorVerifiedFalse();

    /**
     * Récupère les utilisateurs avec 2FA activé et des codes de backup disponibles
     */
    @Query("{ 'twoFactorEnabled': true, 'backupCodes': { $ne: [] } }")
    List<User> findEnabledUsersWithBackupCodes();

    /**
     * Récupère les utilisateurs avec 2FA activé mais sans codes de backup
     */
    @Query("{ 'twoFactorEnabled': true, 'backupCodes': { $eq: [] } }")
    List<User> findEnabledUsersWithoutBackupCodes();

    // ==========================================
    // ✅ 2FA - RECHERCHES COMBINÉES AVANCÉES
    // ==========================================

    /**
     * Recherche des utilisateurs par nom, prénom, email avec filtre 2FA
     */
    @Query("{ $and: [ " +
            "{ $or: [ " +
            "  { 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'email': { $regex: ?0, $options: 'i' } } " +
            "] }, " +
            "{ 'twoFactorEnabled': ?1 } " +
            "] }")
    List<User> searchUsersWithTwoFactorFilter(String query, boolean twoFactorEnabled);

    /**
     * Recherche des utilisateurs actifs par nom, prénom, email avec filtre 2FA
     */
    @Query("{ $and: [ " +
            "{ $or: [ " +
            "  { 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "  { 'email': { $regex: ?0, $options: 'i' } } " +
            "] }, " +
            "{ 'active': true }, " +
            "{ 'twoFactorEnabled': ?1 } " +
            "] }")
    List<User> searchActiveUsersWithTwoFactorFilter(String query, boolean twoFactorEnabled);

    /**
     * Récupère les utilisateurs par rôle et statut 2FA avec tri
     */
    List<User> findByRoleIdAndTwoFactorEnabledOrderByCreatedAtDesc(String roleId, boolean twoFactorEnabled);

    /**
     * Récupère les utilisateurs par rôle, actif et statut 2FA
     */
    List<User> findByRoleIdAndActiveTrueAndTwoFactorEnabledTrue(String roleId);

    /**
     * Récupère les utilisateurs par rôle, actif et 2FA désactivé
     */
    List<User> findByRoleIdAndActiveTrueAndTwoFactorEnabledFalse(String roleId);

    // ==========================================
    // ✅ 2FA - MISE À JOUR EN MASSE (UTILISÉ PAR LE RH)
    // ==========================================

    /**
     * Met à jour le statut 2FA pour tous les utilisateurs d'un rôle
     * (Utilisé en combinaison avec @Modifying)
     */
    @Query("{ 'roleId': ?0 }")
    List<User> findAllByRoleId(String roleId);

    /**
     * Trouve tous les utilisateurs éligibles pour l'activation du 2FA
     * (Actifs, non verrouillés, 2FA désactivé)
     */
    @Query("{ $and: [ " +
            "{ 'active': true }, " +
            "{ 'locked': false }, " +
            "{ 'twoFactorEnabled': false } " +
            "] }")
    List<User> findEligibleForTwoFactor();
}