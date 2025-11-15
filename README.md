# 🔐 TokenGuard

> **Authentification JWT Production-Ready** avec Spring Boot, Token Rotation et Rate Limiting

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)](#)

---

## 📋 Aperçu

**TokenGuard** est un service d'authentification JWT complet pour Spring Boot, conçu pour la production avec sécurité renforcée, logging complet et gestion avancée des tokens.

### ✨ Caractéristiques principales

- 🔐 **JWT avec Spring Security** - Authentification stateless et sécurisée
- 🔄 **Token Rotation** - Rotation automatique des refresh tokens
- 🛡️ **Rate Limiting** - Protection contre les attaques brute-force
- 📝 **Validation complète** - Validation des entrées utilisateur
- 🔍 **Logging audit trail** - Traçabilité complète des opérations
- 🚀 **6 Endpoints** - API RESTful complète
- 📚 **Documentation** - Guides complets et exemples curl
- ⚙️ **Configuration CORS** - Gestion des requêtes cross-origin
- 🧹 **Cleanup automatique** - Suppression des tokens expirés (quotidienne)
- 🎯 **Production-ready** - 95% prêt pour la production

---

## 🚀 Démarrage rapide

### 1️⃣ Cloner le repository

```bash
git clone https://github.com/georgesfk/TokenGuard.git
cd TokenGuard
```

### 2️⃣ Configurer l'environnement

Copier `application.yml` et adapter:

```yaml
jwt:
  secret: "votre-secret-256-bits-minimum"
  expiration: 3600000  # 1 heure
```

### 3️⃣ Démarrer l'application

```bash
mvn spring-boot:run
```

### 4️⃣ Tester les endpoints

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'

# Réponse
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

Voir [QUICKSTART.md](QUICKSTART.md) pour plus d'exemples.

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICKSTART.md](QUICKSTART.md) | Démarrage en 5 minutes |
| [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) | Guide complet d'implémentation |
| [DEPENDENCIES.md](DEPENDENCIES.md) | Dépendances Maven |
| [BEFORE_AFTER.md](BEFORE_AFTER.md) | Comparaison avant/après |
| [RESUME_FINAL.md](RESUME_FINAL.md) | Résumé des améliorations |

---

## 🔐 Endpoints

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/auth/health` | GET | ❌ | Vérifier la santé |
| `/api/auth/login` | POST | ❌ | Se connecter |
| `/api/auth/refresh` | POST | ❌ | Rafraîchir token |
| `/api/auth/logout` | POST | ❌ | Se déconnecter |
| `/api/auth/logout-all` | POST | ✅ | Logout complet |
| `/api/auth/me` | GET | ✅ | Infos utilisateur |

---

## 🛡️ Sécurité

### Implémenté

✅ **Authentification JWT** - Tokens signés avec HMAC-SHA256  
✅ **Token Rotation** - Rotation automatique des refresh tokens  
✅ **Rate Limiting** - 5 tentatives, lockout 15 minutes  
✅ **Validation entrées** - Username/password validés  
✅ **Révocation tokens** - Soft delete avec statut révoqué  
✅ **Logout complet** - Déconnexion de tous les appareils  
✅ **CORS configuré** - Gestion des requêtes cross-origin  
✅ **Logging complet** - Audit trail de toutes les opérations  
✅ **Exception handling** - Gestion globale avec GlobalExceptionHandler  

### Recommandations

- ✅ Utiliser HTTPS en production
- ✅ Configurer un secret JWT robuste (256+ bits)
- ✅ Implémenter `UserDetailsService` avec votre BD
- ✅ Monitorer les logs d'authentification
- ✅ Configurer des alertes de sécurité

---

## 📁 Structure du projet

```
src/main/java/com/auth/
├── config/              # Configuration Spring
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java
│   ├── LoggingConfig.java
│   ├── SchedulingConfig.java
│   └── SecurityConfig.java
├── controller/          # Endpoints REST
│   └── AuthController.java
├── dto/                 # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── RefreshTokenRequest.java
│   └── UserInfoResponse.java
├── exception/           # Exceptions personnalisées
│   ├── InvalidCredentialsException.java
│   ├── InvalidRefreshTokenException.java
│   ├── RefreshTokenExpiredException.java
│   └── TooManyAuthAttemptsException.java
├── model/              # Entités JPA
│   └── RefreshToken.java
├── security/           # Sécurité JWT
│   ├── JwtAccessDeniedHandler.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAuthenticationFilter.java
├── service/            # Services métier
│   ├── AuthService.java
│   ├── AuthResponse.java
│   ├── JwtUtil.java
│   ├── JwtUtilImpl.java
│   ├── RefreshTokenRepository.java
│   └── RefreshTokenService.java
└── util/               # Utilitaires
    ├── RateLimitingUtil.java
    └── ValidationUtil.java
```

---

## 🔧 Configuration

### application.yml (Développement)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver

jwt:
  secret: "your-secret-key-minimum-256-bits-long"
  expiration: 3600000  # 1 heure

logging:
  level:
    com.auth: DEBUG
```

### application-prod.yml (Production)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000

logging:
  level:
    com.auth: INFO
```

---

## 📦 Dépendances

- **Java 17+**
- **Spring Boot 3.1.5+**
- **Spring Security 6+**
- **JJWT 0.12.3** (JWT)
- **H2 Database** (dev) / **PostgreSQL** (prod)
- **Lombok** (productivité)

Voir [pom.xml](pom.xml) pour la liste complète.

---

## 📊 Exemple complet

### 1. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'
```

**Réponse:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Utiliser le token

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. Rafraîchir

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

### 4. Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

---

## 🧪 Tests

### Avec curl

```bash
# Health check
curl http://localhost:8080/api/auth/health

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'
```

### Avec Postman

Voir [QUICKSTART.md](QUICKSTART.md) pour la collection Postman.

---

## 🐛 Troubleshooting

### "Token invalide"
- Vérifiez le format: `Bearer <token>`
- Vérifiez que le token n'a pas expiré
- Vérifiez le secret JWT

### "Trop de tentatives"
- Attendez 15 minutes ou utilisez un autre utilisateur
- Vérifiez le mot de passe

### Base de données
- Dev: H2 en mémoire (défaut)
- Prod: PostgreSQL à configurer

---

## 📈 Roadmap

- [ ] 2FA (Two-Factor Authentication)
- [ ] OAuth2/OpenID Connect
- [ ] Device fingerprinting
- [ ] Audit table complète
- [ ] Dashboard admin
- [ ] Mobile app support

---

## 📄 License

MIT License - Voir [LICENSE](LICENSE) pour plus de détails.

---

## 🤝 Contributing

Les contributions sont bienvenues! Ouvrez une issue ou un PR.

---

## 💬 Support

Pour toute question ou problème:
- Ouvrir une [issue](https://github.com/georgesfk/TokenGuard/issues)
- Voir la [documentation](IMPLEMENTATION_GUIDE.md)

---

## 👨‍💻 Auteur

**George** - [@georgesfk](https://github.com/georgesfk)

---

## ⭐ Show your support

Si ce projet vous a aidé, n'hésitez pas à lui donner une ⭐ sur GitHub!

---

**TokenGuard** - *Authentification JWT sécurisée et production-ready* 🔐