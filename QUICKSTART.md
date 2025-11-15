# ⚡ Quick Start Guide

## 5 minutes pour démarrer

### Étape 1: Ajouter les dépendances Maven

Copier dans votre `pom.xml` (voir `pom.xml` fourni) :

```xml
<!-- Spring Boot & Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database (H2 for dev) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### Étape 2: Configurer application.yml

Copier `src/main/resources/application.yml` (déjà fourni)

### Étape 3: Copier tous les fichiers Java

Structure à respecter :
```
src/main/java/com/auth/
├── config/          (4 fichiers)
├── controller/      (1 fichier)
├── exception/       (4 fichiers)
├── model/           (1 fichier)
├── service/         (6 fichiers)
└── util/            (2 fichiers)
```

### Étape 4: Implémenter UserDetailsService (optionnel)

Si vous avez votre propre système d'utilisateurs :

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired UserRepository userRepo;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            new ArrayList<>()
        );
    }
}
```

### Étape 5: Démarrer l'application

```bash
mvn spring-boot:run
```

### Étape 6: Tester les endpoints

```bash
# 1. Login
curl -X POST "http://localhost:8080/api/auth/login?username=user&password=password"

# Réponse:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIs...",
#   "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
# }

# 2. Refresh Token (utiliser le refreshToken reçu)
curl -X POST "http://localhost:8080/api/auth/refresh?refreshToken=550e8400-e29b-41d4-a716-446655440000"

# 3. Logout
curl -X POST "http://localhost:8080/api/auth/logout?refreshToken=550e8400-e29b-41d4-a716-446655440000"

# 4. Logout All Devices
curl -X POST "http://localhost:8080/api/auth/logout-all?username=user"
```

---

## 🎯 Points clés

✅ **Validation** - Tous les paramètres sont validés
✅ **Sécurité** - Rate limiting, token rotation, révocation
✅ **Logging** - Tous les événements sont loggés
✅ **Erreurs** - Gestion personnalisée avec GlobalExceptionHandler
✅ **Cleanup** - Automatique (quotidien à 2h du matin)

---

## 📚 Fichiers importants

- `RESUME_FINAL.md` - Vue d'ensemble complète
- `IMPLEMENTATION_GUIDE.md` - Guide détaillé
- `DEPENDENCIES.md` - Toutes les dépendances
- `pom.xml` - Configuration Maven

---

## ⚠️ En cas de problème

### Base de données
- Dev: H2 en mémoire (`jdbc:h2:mem:testdb`)
- Prod: PostgreSQL à configurer

### JWT
- Secret minimum 256 bits
- Expiration par défaut 1 heure

### Authentication
- Implémenter `UserDetailsService`
- Configurer `PasswordEncoder` (BCrypt fourni)

---

## 🚀 C'est tout!

Vous avez maintenant un service d'authentification production-ready!

Questions? Voir `IMPLEMENTATION_GUIDE.md`
