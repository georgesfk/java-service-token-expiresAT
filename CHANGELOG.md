# Changelog

Tous les changements notables de ce projet seront documentés dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
et ce projet suit [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-15

### ✨ Added (Ajouté)

- **JWT Authentication** - Implémentation complète avec Spring Security
- **Token Rotation** - Rotation automatique des refresh tokens
- **Rate Limiting** - Protection brute-force (5 tentatives, 15 min lockout)
- **RefreshTokenService** - Service dédié pour la gestion des tokens
- **JwtAuthenticationFilter** - Filtre pour valider JWT sur chaque requête
- **JwtAuthenticationEntryPoint** - Gestion des erreurs d'authentification
- **JwtAccessDeniedHandler** - Gestion des accès refusés (403)
- **SecurityConfig** - Configuration Spring Security complète
- **CorsConfig** - Gestion CORS configurable
- **GlobalExceptionHandler** - Gestion centralisée des exceptions
- **ValidationUtil** - Validation des entrées utilisateur
- **RateLimitingUtil** - Implémentation du rate limiting
- **4 Exceptions personnalisées** - InvalidCredentialsException, InvalidRefreshTokenException, RefreshTokenExpiredException, TooManyAuthAttemptsException
- **3 DTOs** - LoginRequest, RefreshTokenRequest, UserInfoResponse
- **6 Endpoints REST** - login, refresh, logout, logout-all, me, health
- **Logging complet** - @Slf4j avec audit trail
- **Cleanup automatique** - @Scheduled suppression des tokens expirés
- **Documentation complète** - 6 fichiers guides + exemples
- **Indexes BD** - Optimisation des performances
- **Configuration Maven** - pom.xml avec toutes les dépendances

### 🔐 Security Features (Sécurité)

- ✅ JWT signés avec HMAC-SHA256
- ✅ Token rotation avec suppression de l'ancien
- ✅ Soft delete pour révocation
- ✅ Rate limiting à 5 tentatives
- ✅ Lockout 15 minutes après dépassement
- ✅ Validation stricte des paramètres
- ✅ Logging audit trail complet
- ✅ Gestion CORS sécurisée

### 📚 Documentation

- **QUICKSTART.md** - Démarrage en 5 minutes
- **IMPLEMENTATION_GUIDE.md** - Guide d'implémentation complet
- **DEPENDENCIES.md** - Dépendances Maven détaillées
- **RESUME_FINAL.md** - Résumé des améliorations
- **BEFORE_AFTER.md** - Comparaison avant/après
- **CHECKLIST.md** - Checklist de validation
- **README.md** - Présentation du projet
- **CONTRIBUTING.md** - Guide de contribution
- **LICENSE** - MIT License

### 🛠️ Configuration

- **application.yml** - Configuration développement
- **application-prod.yml** - Configuration production
- **pom.xml** - Maven avec toutes dépendances
- **.gitignore** - Fichiers à ignorer

---

## Roadmap

### 🔜 Prochaines versions

#### v1.1.0
- [ ] 2FA (Two-Factor Authentication)
- [ ] Email verification
- [ ] Password reset flow

#### v1.2.0
- [ ] OAuth2 support
- [ ] OpenID Connect
- [ ] Social login (Google, GitHub, etc)

#### v2.0.0
- [ ] Device management
- [ ] Session tracking
- [ ] Audit dashboard
- [ ] Admin API

---

## Support

Pour les questions ou problèmes:
- 📧 Ouvrir une [issue](https://github.com/georgesfk/TokenGuard/issues)
- 💬 Discuter sur [discussions](https://github.com/georgesfk/TokenGuard/discussions)

---

## Remerciements

Merci à la communauté Spring Boot et à tous les contributeurs! 🙏

---

**TokenGuard** - Authentification JWT sécurisée et production-ready 🔐
