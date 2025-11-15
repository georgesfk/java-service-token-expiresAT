# Service d'Authentification Java - Guide Complet

## 🎯 Vue d'ensemble

Ce service fournit une implémentation complète d'authentification avec JWT et refresh tokens, incluant :

- ✅ Validation des entrées
- ✅ Gestion d'erreurs personnalisées
- ✅ Rate limiting et protection brute-force
- ✅ Logging complet
- ✅ Rotation des refresh tokens
- ✅ Révocation de session
- ✅ Cleanup automatique des tokens expirés
- ✅ Gestion globale des exceptions

## 📁 Structure du projet

```
src/main/java/com/auth/
├── config/              # Configurations Spring
│   ├── GlobalExceptionHandler.java
│   ├── LoggingConfig.java
│   └── SchedulingConfig.java
├── controller/          # Contrôleurs REST
│   └── AuthController.java
├── exception/           # Exceptions personnalisées
│   ├── InvalidCredentialsException.java
│   ├── InvalidRefreshTokenException.java
│   ├── RefreshTokenExpiredException.java
│   └── TooManyAuthAttemptsException.java
├── model/              # Entités JPA
│   └── RefreshToken.java
├── service/            # Services métier
│   ├── AuthService.java
│   ├── AuthResponse.java
│   ├── JwtUtil.java
│   ├── RefreshTokenRepository.java
│   └── RefreshTokenService.java
└── util/               # Utilitaires
    ├── RateLimitingUtil.java
    └── ValidationUtil.java
```

## 🔐 Flux d'authentification

### 1. Login
```
POST /api/auth/login?username=user&password=pass
  ↓
Validation des entrées
  ↓
Vérification rate limiting
  ↓
Authentification (AuthenticationManager)
  ↓
Création Access Token (JWT)
  ↓
Création Refresh Token (BD)
  ↓
Response: { accessToken, refreshToken }
```

### 2. Refresh Token
```
POST /api/auth/refresh?refreshToken=xxx
  ↓
Validation du refresh token
  ↓
Vérification expiration
  ↓
Nouveau Access Token généré
  ↓
Rotation: ancien token supprimé, nouveau créé
  ↓
Response: { newAccessToken, newRefreshToken }
```

### 3. Logout
```
POST /api/auth/logout?refreshToken=xxx
  ↓
Révocation du token
  ↓
Token marqué comme révoqué en BD
```

### 4. Logout All Devices
```
POST /api/auth/logout-all?username=user
  ↓
Tous les tokens de l'utilisateur sont révoqués
  ↓
Déconnexion de tous les appareils
```

## 🛡️ Sécurité

### Rate Limiting
- **Max tentatives** : 5 échouées
- **Durée lockout** : 15 minutes
- **Réinitialisation** : À chaque succès

### Validation
- Username minimum 3 caractères
- Password minimum 6 caractères
- Paramètres non-vides obligatoires

### Gestion d'erreurs
- Exceptions métier spécifiques (InvalidRefreshTokenException, etc.)
- Logging détaillé de toutes les opérations
- Réponses d'erreur standardisées

### Token Rotation
- Ancien refresh token supprimé après rotation
- Nouveau token généré avec expiration 30 jours
- Prévient la réutilisation de tokens compromis

## 🔄 Cleanup automatique

Une tâche scheduled s'exécute quotidiennement à 2h du matin :
```java
@Scheduled(cron = "0 0 2 * * *")
void cleanupExpiredTokens()
```

Supprime tous les tokens expirés de la base de données.

## 📊 Logging

Tous les événements importants sont loggés :

```
INFO  - Tentative de connexion pour l'utilisateur: john
INFO  - Authentification réussie pour: john
INFO  - Connexion réussie pour: john
WARN  - Identifiants invalides pour: john
WARN  - Trop de tentatives pour: john
INFO  - Logout complet pour l'utilisateur: john
```

## 🚀 Utilisation

### Configuration dans votre projet

1. **Ajouter les dépendances** (voir `DEPENDENCIES.md`)
2. **Implémenter JwtUtil** :
```java
@Component
public class JwtUtilImpl implements JwtUtil {
    @Override
    public String generateAccessToken(String username) {
        // Implémentation JWT
    }
}
```

3. **Configurer AuthenticationManager** dans SecurityConfig
4. **Configurer PasswordEncoder** dans SecurityConfig
5. **Configurer la BD** dans `application.yml`

### Endpoints

#### Login
```bash
POST /api/auth/login?username=john&password=pass123
Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Refresh
```bash
POST /api/auth/refresh?refreshToken=550e8400-e29b-41d4-a716-446655440000
Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "660f8400-e29b-41d4-a716-446655440001"
}
```

#### Logout
```bash
POST /api/auth/logout?refreshToken=550e8400-e29b-41d4-a716-446655440000
Response:
"Logout réussi"
```

#### Logout All
```bash
POST /api/auth/logout-all?username=john
Response:
"Logout complet réussi"
```

## 📋 Gestion des erreurs

### 401 Unauthorized
- Identifiants invalides
- Token expiré
- Token introuvable

### 400 Bad Request
- Paramètres vides
- Validation échouée

### 429 Too Many Requests
- Trop de tentatives
- Inclut `retryAfterSeconds`

### 500 Internal Server Error
- Erreur serveur inattendue

## 🔍 Monitoring

### Fichiers de log
```
logs/app.log
```

### Requêtes HTTP loggées
Toutes les requêtes incluent :
- Client IP
- Query string
- Payload (10KB max)
- Headers

### Audit trail
Tous les événements d'authentification sont tracés :
- Tentatives réussies
- Tentatives échouées
- Tokens révoqués
- Cleanups

## 🧪 Tests

Voir les endpoints avec curl :

```bash
# Login
curl -X POST "http://localhost:8080/api/auth/login?username=john&password=pass123"

# Refresh
curl -X POST "http://localhost:8080/api/auth/refresh?refreshToken=xxx"

# Logout
curl -X POST "http://localhost:8080/api/auth/logout?refreshToken=xxx"

# Logout All
curl -X POST "http://localhost:8080/api/auth/logout-all?username=john"
```

## 📝 Notes importantes

1. **JwtUtil** : Doit être implémenté selon votre configuration JWT
2. **PasswordEncoder** : À configurer dans SecurityConfig
3. **AuthenticationManager** : À configurer dans SecurityConfig
4. **Base de données** : H2 par défaut, PostgreSQL pour prod
5. **Scheduling** : Nécessite `@EnableScheduling` en configuration

## 🔗 Améliorations futures

- [ ] Double authentification (2FA)
- [ ] Tokens avec claims personnalisés
- [ ] Audit table complète
- [ ] Alertes sur suspicion d'activité
- [ ] Device fingerprinting
- [ ] IP whitelist
