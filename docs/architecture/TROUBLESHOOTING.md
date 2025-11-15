# 🔧 Troubleshooting & FAQ - TokenGuard

## 1. Problèmes Courants d'Authentification

### ❌ "Invalid JWT"

**Symptôme:**
```
HTTP 401 Unauthorized
{
  "error": "INVALID_JWT",
  "message": "JWT validation failed"
}
```

**Causes possibles:**
1. Token expiré
2. Signature invalide
3. Secret JWT différent

**Solutions:**

```bash
# Vérifier la date du token
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# Générer un nouveau token avec login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'

# Vérifier le secret JWT en application.yml
# Assurez-vous que jwt.secret est identique entre instances
```

**Vérifier la validité du token (JWT.io):**
```javascript
// Copier le token dans https://jwt.io
// et vérifier:
// 1. Header: alg=HS256, typ=JWT
// 2. Payload: exp timestamp valide
// 3. Verification: même secret utilisé
```

---

### ❌ "Token Expired"

**Symptôme:**
```json
{
  "error": "TOKEN_EXPIRED",
  "message": "JWT token has expired"
}
```

**Solutions:**

```bash
# Utiliser le refresh token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'

# Ou se reconnecter
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'
```

**Configuration:**

```yaml
# application.yml - Augmenter l'expiration si besoin
jwt:
  secret: "your_256_bit_secret"
  expiration: 3600000  # En millisecondes (1h = 3600000)
```

---

### ❌ "Too Many Auth Attempts"

**Symptôme:**
```json
{
  "error": "TOO_MANY_ATTEMPTS",
  "message": "Too many failed authentication attempts. Please try again in 15 minutes",
  "retryAfter": 900
}
```

**Causes:**
- 5 tentatives échouées en 15 minutes
- Brute force attack

**Solutions:**

```bash
# Attendre 15 minutes (900 secondes)
# Ou redémarrer l'application (remet les compteurs)

# Vérifier les logs
tail -f logs/tokenguard.log | grep RateLimitingUtil

# Augmenter la limite si besoin (dans RateLimitingUtil.java)
private static final int MAX_ATTEMPTS = 5;  // Changer cette valeur
```

---

### ❌ "Refresh Token Not Found"

**Symptôme:**
```json
{
  "error": "INVALID_REFRESH_TOKEN",
  "message": "Refresh token not found or invalid"
}
```

**Causes:**
1. Token révoqué après logout
2. Token expiré (> 30 jours)
3. Token inexistant en base

**Solutions:**

```bash
# Vérifier si le token existe en BD
psql -U tokenguard -h localhost tokenguard
SELECT * FROM refresh_token WHERE token = 'YOUR_TOKEN';

# Si vide: le token a été supprimé (revoqué/expiré)
# Solution: Refaire un login

# Vérifier les tokens d'un utilisateur
SELECT token, expires_at, revoked FROM refresh_token 
WHERE username = 'john' 
ORDER BY created_at DESC;
```

---

## 2. Problèmes de Base de Données

### ❌ "Connection refused to PostgreSQL"

**Symptôme:**
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Solutions:**

```bash
# 1. Vérifier que PostgreSQL est en cours d'exécution
ps aux | grep postgres

# 2. Tester la connexion
psql -U tokenguard -h localhost -W

# 3. Vérifier la configuration
cat application.yml | grep datasource

# 4. Utiliser H2 pour dev (par défaut)
# Aucune configuration nécessaire, l'application crée la BD

# 5. Redémarrer PostgreSQL
sudo systemctl restart postgresql

# 6. Vérifier les logs Docker
docker logs postgres_container

# 7. Vérifier le port
netstat -tulpn | grep 5432
```

---

### ❌ "Table 'refresh_token' not found"

**Symptôme:**
```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "REFRESH_TOKEN" not found
```

**Solutions:**

```bash
# 1. Vérifier spring.jpa.hibernate.ddl-auto
# application.yml doit avoir:
spring:
  jpa:
    hibernate:
      ddl-auto: create  # ou update

# 2. Si table existe mais n'est pas trouvée
# Recréer la base:
DROP TABLE IF EXISTS refresh_token;

# Ou via application:
# SET spring.jpa.hibernate.ddl-auto=create-drop
# puis relancer

# 3. Vérifier la DB utilisée
SELECT DATABASE();  # MySQL
\l  # PostgreSQL
```

---

### ❌ "Unique constraint violation"

**Symptôme:**
```
org.postgresql.util.PSQLException: ERROR: duplicate key value violates 
unique constraint "refresh_token_token_key"
```

**Causes:**
- Même token généré deux fois (très rare)
- Données corruptues

**Solutions:**

```bash
# Nettoyer les doublons
DELETE FROM refresh_token 
WHERE id NOT IN (
  SELECT MIN(id) FROM refresh_token 
  GROUP BY token
);

# Vérifier l'intégrité
SELECT COUNT(*), token 
FROM refresh_token 
GROUP BY token 
HAVING COUNT(*) > 1;

# Si plusieurs: il y a un bug
# Contacter le support ou créer une issue
```

---

## 3. Problèmes de Sécurité

### ❌ "CORS Error"

**Symptôme:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/auth/login' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solutions:**

```yaml
# application.yml - Configurer CORS
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:4200
    - https://example.com
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
  allowed-headers:
    - Content-Type
    - Authorization
  allow-credentials: true
  max-age: 3600
```

**Ou via CorsConfig.java:**

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

---

### ❌ "Invalid Credentials"

**Symptôme:**
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid username or password"
}
```

**Causes:**
1. Mauvais mot de passe
2. Utilisateur inexistant
3. Utilisateur désactivé

**Solutions:**

```bash
# 1. Vérifier que l'utilisateur existe en BD
psql -U tokenguard
SELECT * FROM users WHERE username = 'john';

# 2. Tester les credentials
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}'

# 3. Vérifier le hash du mot de passe
# Spring utilise BCrypt, ne peut pas être décodé
# Solution: Reset le mot de passe via admin panel

# 4. Vérifier si l'utilisateur est actif
SELECT enabled FROM users WHERE username = 'john';  # doit être true
```

---

## 4. Problèmes de Performance

### 🐢 "Slow Login Response"

**Symptôme:**
- Login prend > 5 secondes
- CPU élevé pendant l'authentification

**Causes:**
1. BCrypt trop lent (configurable)
2. Base de données lente
3. Manque de ressources

**Solutions:**

```java
// SecurityConfig.java - Ajuster BCrypt strength
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);  // 4-15, plus = plus lent
}

// 4: très rapide (dev)
// 10: balance (prod)
// 15: très sécurisé mais lent
```

```bash
# Vérifier les indexes DB
psql tokenguard
SELECT * FROM pg_indexes WHERE tablename = 'refresh_token';

# Indexes doivent exister sur:
# - token
# - username
# - expires_at
```

```yaml
# application.yml - Pool de connexions
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
```

---

### 🐢 "High Memory Usage"

**Symptôme:**
- Utilisation mémoire > 1GB
- Heap size atteint la limite

**Solutions:**

```bash
# Augmenter la heap
java -Xmx2g -Xms1g -jar tokenguard.jar

# Ou dans docker-compose.yml
environment:
  JAVA_OPTS: "-Xmx2g -Xms1g"

# Vérifier les fuites mémoire
# Utiliser JProfiler ou YourKit

# Cleanup scheduled task (tous les jours à 2h)
# Nettoie automatiquement les tokens expirés
```

---

## 5. Problèmes Docker

### ❌ "Container keeps restarting"

**Symptôme:**
```
Restarting TokenGuard container (1/5)
```

**Solutions:**

```bash
# Vérifier les logs
docker logs tokenguard_container

# Problèmes courants:
# 1. Port déjà utilisé
docker ps | grep 8080
kill -9 <PID>

# 2. Variables d'environnement manquantes
docker run -e JWT_SECRET="..." tokenguard:latest

# 3. Base de données inaccessible
docker-compose up db
docker-compose up app  # Après que DB soit UP

# 4. Insufficient memory
docker stats tokenguard_container
# Augmenter --memory limit
```

---

### ❌ "Health check failing"

**Symptôme:**
```
Health status: Unhealthy
Application startup failed
```

**Solutions:**

```bash
# Test manuel de l'health endpoint
docker exec tokenguard_container \
  curl http://localhost:8080/api/auth/health

# Logs complets
docker logs -f tokenguard_container

# Donner plus de temps au startup
# Dans Dockerfile:
HEALTHCHECK --start-period=60s ...

# Vérifier que tous les services sont up
docker-compose ps
# Tous doivent être "healthy" ou "running"
```

---

## 6. Problèmes de Déploiement

### ❌ "Pod keeps crashing in K8s"

**Symptôme:**
```
Pod is in state Pending/CrashLoopBackOff
```

**Solutions:**

```bash
# Vérifier le statut
kubectl describe pod tokenguard-xxx -n production

# Voir les logs
kubectl logs -f deployment/tokenguard -n production

# Common issues:
# 1. Image pull error
kubectl get events -n production

# 2. Resource request non satisfait
kubectl top nodes  # Voir disponibilité
kubectl describe node node-name

# 3. ConfigMap/Secret manquant
kubectl get configmaps -n production
kubectl get secrets -n production

# 4. Port déjà utilisé (très rare en K8s)
# Changer le port dans deployment.yaml

# Debug interactif
kubectl run -it --rm debug --image=alpine --restart=Never -- sh
# Tester la connectivité:
# nc -zv tokenguard-service 8080
```

---

## 7. FAQ

### Q: Comment changer le secret JWT?

**R:**
```bash
# 1. Générer nouveau secret (256+ bits)
openssl rand -base64 32

# 2. Mettre à jour application.yml
jwt:
  secret: "NEW_SECRET_HERE"

# 3. Les tokens existants deviendront invalides!
# Avertir les utilisateurs de se reconnecter

# 4. Redéployer l'application
mvn spring-boot:run
```

---

### Q: Comment faire un logout sans token?

**R:**
```bash
# Logout nécessite le refresh token
# Sans token: la session est déjà terminée côté client

# Si besoin de forcer la révocation côté serveur:
# 1. Admin panel qui révoque tous les tokens d'un user
# 2. Ou appel administrateur SQL:
UPDATE refresh_token 
SET revoked = true, revoked_at = NOW()
WHERE username = 'john';
```

---

### Q: Les tokens persistent après redémarrage?

**R:**
```
OUI - Les tokens sont en base de données (PostgreSQL ou H2)
- H2: Données perdurent en mémoire sauf si restart
- PostgreSQL: Données persistent sur disque

Pour réinitialiser (dev):
truncate table refresh_token;

Pour prod: ne pas faire! Perte de sessions utilisateurs.
```

---

### Q: Puis-je utiliser TokenGuard sans JWT?

**R:**
```
NON - TokenGuard est basé sur JWT stateless.

Alternatives:
1. Session-based: Utiliser Spring Session + Redis
2. OAuth2: Intégrer OAuthServer (Google, GitHub)
3. Personaliser: Fork le repo et modifier JwtUtilImpl

Recommandation: JWT est plus scalable pour les microservices.
```

---

### Q: Comment supporter plusieurs tenants?

**R:**
```java
// Ajouter tenant_id dans le JWT payload
String generateAccessToken(String username, String tenantId) {
    return Jwts.builder()
        .subject(username)
        .claim("tenantId", tenantId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
}

// Valider tenant lors du refresh
if (!jwtUtil.getTenantIdFromToken(token).equals(currentTenantId)) {
    throw new SecurityException("Tenant mismatch");
}
```

---

### Q: Comment intégrer une 2FA?

**R:**
```java
// 1. Ajouter TOTP (Time-based One-Time Password)
// Dépendance: org.jboss.aerogear:aerogear-otp-java

// 2. Modify AuthService.login():
public AuthResponse login(String username, String password, String totp) {
    // ... authentifier username/password
    
    // Vérifier TOTP
    if (!totpService.verify(username, totp)) {
        throw new InvalidCredentialsException("Invalid TOTP");
    }
    
    // Procéder au token
    return createTokens(username);
}

// 3. Frontend: demander TOTP après login
```

---

### Q: Comment monitorer les tentatives de login échouées?

**R:**
```java
@Slf4j
@Service
public class AuthService {
    public AuthResponse login(String username, String password) {
        try {
            // ...
        } catch (InvalidCredentialsException e) {
            log.warn("Failed login attempt for user: {}", username);
            // Envoyer métrique
            meterRegistry.counter("auth.login.failed", 
                "username", username).increment();
        }
    }
}

// Dashboard Prometheus:
rate(auth_login_failed_total[5m])
```

---

## 8. Logs utiles pour debugging

```bash
# Tous les logs Auth (DEBUG level)
tail -f logs/tokenguard.log | grep com.auth

# JWT operations
tail -f logs/tokenguard.log | grep JwtUtil

# Database operations
tail -f logs/tokenguard.log | grep RefreshToken

# Security events
tail -f logs/tokenguard.log | grep SecurityContext

# Rate limiting
tail -f logs/tokenguard.log | grep RateLimitingUtil

# Erreurs d'authentification
tail -f logs/tokenguard.log | grep "AUTH\|UNAUTHORIZED\|FORBIDDEN"
```

---

## 9. Contacts Support

**Pour les issues:**
- GitHub Issues: https://github.com/georgesfk/tokenguard/issues
- Email: support@tokenguard.dev
- Slack: #tokenguard-support

**Documentation:**
- README: ./README.md
- Quick Start: ./QUICKSTART.md
- API Guide: ./IMPLEMENTATION_GUIDE.md
- Architecture: ./docs/architecture/ARCHITECTURE.md

---

**Besoin d'aide? Consultez cette page en premier! 🆘**
