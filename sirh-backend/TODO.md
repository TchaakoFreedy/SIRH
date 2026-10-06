# TODO - Phase 1 : Authentification & Autorisation (SIRH)

## Étape 1 — Audit & alignement spec
- [ ] Vérifier les entités MongoDB : `User`, `Role`, `Permission` (champs requis + annotations)
- [ ] Vérifier DTOs existants : `LoginRequest`, `RefreshTokenRequest`, `AuthResponse`, `UserDTO`

## Étape 2 — Security JWT
- [ ] Mettre à jour `JwtService` : utiliser `jwt.secret` (pas clé en dur)
- [ ] Mettre à jour `JwtService` : claims `roleLevel` + extraction complète
- [ ] S’assurer que `isRefreshToken()` fonctionne et utilise le claim `type`
- [ ] Ajouter logging d’erreurs robuste

## Étape 3 — Filtre JWT
- [ ] Mettre à jour `JwtAuthenticationFilter` : authorities `ROLE_...` + permissions
- [ ] S’assurer que le contexte Spring Security est correctement peuplé

## Étape 4 — Security method & annotations
- [ ] Vérifier et corriger `PermissionEvaluator`
- [ ] Vérifier et aligner les annotations custom (`@IsRH`, `@IsRHOrAbove`, `@IsManagerOrAbove`, `@IsEmployeeOnly`, etc.)
- [ ] Implémenter la logique hiérarchie via `roleLevel`/règles (conforme spec)

## Étape 5 — AuthController & AuthService
- [ ] Corriger `AuthController.logout` : appeler `AuthService.logout` à partir du token Authorization
- [ ] Corriger `refresh` : rotation/invalidation selon spec (au minimum : validation refresh token)
- [ ] Validation DTO avec `@Valid`
- [ ] Gestion d’exceptions : messages clairs en français

## Étape 6 — DataLoader
- [ ] Corriger DataLoader : admin rôle **RH** (pas DIRECTION)
- [ ] Corriger rôles + permissions exactement selon hiérarchie demandée
- [ ] S’assurer que `hierarchyLevel` et `permissionIds` sont correctement renseignés
- [ ] Éviter doublons via `existsBy...`

## Étape 7 — SecurityConfig
- [ ] Vérifier `/api/auth/login`, `/api/auth/refresh`, swagger en public
- [ ] Stateless session
- [ ] Activer `@EnableMethodSecurity` si nécessaire

## Étape 8 — Maven & dépendances
- [ ] Nettoyer `pom.xml` (doublons, artefacts manquants)
- [ ] Ajouter Swagger/OpenAPI si absent

## Étape 9 — Tests
- [ ] Ajouter tests autorisation : 200 vs 403 selon rôle/permission
- [ ] Tests JWT (mock) pour `@IsRHOrAbove`, `@HasPermission`, `@IsEmployeeOnly`

## Étape 10 — Postman collection (si demandé)
- [ ] Générer collection Postman complète


