# 🎯 Design Patterns dans TokenGuard

## 1. Service Layer Pattern

### Description
Séparer la logique métier de la présentation et du stockage.

### Implémentation

```java
// Service
@Service
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    
    public AuthResponse login(String username, String password) {
        // Logique métier centralisée
    }
}

// Repository
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}

// Controller
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Délègue au service
        return ResponseEntity.ok(authService.login(...));
    }
}
```

### Avantages
- ✅ Logique métier isolée
- ✅ Testable indépendamment
- ✅ Réutilisable
- ✅ Maintenable

---

## 2. Filter Pattern

### Description
Chaque requête passe par des filtres pour validation/transformation.

### Implémentation

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
        // 1. Extraire JWT
        String jwt = extractJwtFromRequest(request);
        
        // 2. Valider
        if (jwt != null && !jwtUtil.isTokenExpired(jwt)) {
            // 3. Setter SecurityContext
            String username = jwtUtil.getUsernameFromToken(jwt);
            var userDetails = userDetailsService.loadUserByUsername(username);
            var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        // 4. Continuer la chaîne
        filterChain.doFilter(request, response);
    }
}
```

### Avantages
- ✅ Exécution pour chaque requête
- ✅ Séparation des préoccupations
- ✅ Réutilisable pour d'autres filtres
- ✅ Ordre définissable

---

## 3. Strategy Pattern

### Description
Plusieurs algorithmes encapsulés et interchangeables.

### Implémentation

```java
// Interface (Strategy)
public interface JwtUtil {
    String generateAccessToken(String username);
}

// Implémentation concrète
@Component
public class JwtUtilImpl implements JwtUtil {
    @Override
    public String generateAccessToken(String username) {
        // Implémentation spécifique avec JJWT
        return Jwts.builder()...build();
    }
}

// Utilisation
@Service
public class AuthService {
    private final JwtUtil jwtUtil;  // Dépend de l'interface
    
    public AuthResponse login(String username, String password) {
        String token = jwtUtil.generateAccessToken(username);
        // ...
    }
}
```

### Avantages
- ✅ Flexible et extensible
- ✅ Facile à tester (mock)
- ✅ Permet différentes implémentations

---

## 4. Builder Pattern

### Description
Construire des objets complexes étape par étape.

### Implémentation

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private Long id;
    private String token;
    private String username;
    private Instant expiresAt;
    private Instant createdAt;
    private boolean revoked;
    private Instant revokedAt;
}

// Utilisation
RefreshToken token = RefreshToken.builder()
    .token(UUID.randomUUID().toString())
    .username("john")
    .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
    .createdAt(Instant.now())
    .revoked(false)
    .build();
```

### Avantages
- ✅ API fluide et lisible
- ✅ Valeurs par défaut possibles
- ✅ Immuabilité optionnelle

---

## 5. Decorator Pattern

### Description
Ajouter des responsabilités à un objet dynamiquement.

### Implémentation

```java
// Objet de base
public class AuthService {
    public AuthResponse login(String username, String password) {
        // Logique d'authentification
    }
}

// Décorateurs appliqués par Spring
@Slf4j                              // Logging decorator
@Service                            // Service decorator
@RequiredArgsConstructor            // Constructor decorator
@Transactional                      // Transaction decorator
public class AuthService {
    // ...
}

// Filtre = Décorateur de requête
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Décore chaque requête
}
```

### Avantages
- ✅ Responsabilités orthogonales
- ✅ Composition flexible
- ✅ Pas de modification de la classe de base

---

## 6. Template Method Pattern

### Description
Définir le squelette d'un algorithme, laisser les sous-classes implémenter.

### Implémentation

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Template = flux de configuration
        return http
            .csrf().disable()           // Étape 1
            .cors().and()               // Étape 2
            .sessionManagement()...     // Étape 3
            .exceptionHandling()...     // Étape 4
            .authorizeHttpRequests()... // Étape 5
            .addFilterBefore(...)       // Étape 6
            .build();
    }
}
```

### Avantages
- ✅ Flux prévisible
- ✅ Points d'extension clairs
- ✅ Réduction de la duplication

---

## 7. Singleton Pattern

### Description
Une seule instance d'une classe dans l'application.

### Implémentation

```java
@Service
public class AuthService {
    // Spring gère le singleton
    // Une seule instance par application
}

@Component
public class ValidationUtil {
    // Singleton
}

@Bean
public PasswordEncoder passwordEncoder() {
    // Singleton
    return new BCryptPasswordEncoder();
}
```

### Avantages
- ✅ Économie de mémoire
- ✅ Accès global (via DI)
- ✅ Thread-safe (géré par Spring)

---

## 8. Observer Pattern

### Description
Notifier automatiquement les observateurs de changements.

### Implémentation

```java
// Spring Events
@Service
public class AuthService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public AuthResponse login(String username, String password) {
        // ... authentification
        
        // Publier un événement
        eventPublisher.publishEvent(
            new LoginSuccessEvent(this, username)
        );
        
        return response;
    }
}

// Observer
@Component
public class LoginEventListener {
    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        // Réagir à l'événement
        log.info("Login success: {}", event.getUsername());
    }
}
```

### Avantages
- ✅ Couplage faible
- ✅ Extensibilité
- ✅ Traçabilité

---

## 9. Factory Pattern

### Description
Créer des objets sans spécifier leurs classes concrètes.

### Implémentation

```java
@Component
public class AuthenticationFactory {
    
    public Authentication createAuthentication(
        UserDetails userDetails, 
        Collection<? extends GrantedAuthority> authorities) {
        
        return new UsernamePasswordAuthenticationToken(
            userDetails, null, authorities
        );
    }
}

// Utilisation
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private AuthenticationFactory authFactory;
    
    protected void doFilterInternal(...) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication auth = authFactory.createAuthentication(userDetails, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
```

### Avantages
- ✅ Logique de création centralisée
- ✅ Facile à changer sans impact
- ✅ Testable

---

## 10. Immutable Object Pattern

### Description
Objets qui ne peuvent pas être modifiés après création.

### Implémentation

```java
@Value  // Lombok: immutable avec getter
@AllArgsConstructor
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
}

// Utilisation
AuthResponse response = new AuthResponse(
    accessToken,
    refreshToken
);

// response.getAccessToken()  ✅ OK
// response.setAccessToken()  ❌ Compile error
```

### Avantages
- ✅ Thread-safe
- ✅ Prévisible
- ✅ Cache-friendly

---

## 11. Exception Handling Pattern

### Description
Gestion centralisée des exceptions.

### Implémentation

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(
        InvalidCredentialsException ex) {
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("INVALID_CREDENTIALS", ex.getMessage()));
    }
    
    @ExceptionHandler(TooManyAuthAttemptsException.class)
    public ResponseEntity<?> handleTooManyAttempts(
        TooManyAuthAttemptsException ex) {
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("TOO_MANY_ATTEMPTS", ex.getMessage()));
    }
}
```

### Avantages
- ✅ Gestion cohérente
- ✅ Code plus propre (pas de try-catch partout)
- ✅ Réponses standardisées

---

## 12. Dependency Injection Pattern

### Description
Injecter les dépendances plutôt que les créer.

### Implémentation

```java
@Service
@RequiredArgsConstructor  // Constructor injection via Lombok
public class AuthService {
    // Final fields injectées
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RateLimitingUtil rateLimitingUtil;
    private final ValidationUtil validationUtil;
}

// Alternative: Setter injection
@Service
public class AuthService {
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    public void setRefreshTokenService(RefreshTokenService service) {
        this.refreshTokenService = service;
    }
}
```

### Avantages
- ✅ Testabilité (mock facile)
- ✅ Flexibilité
- ✅ Maintenabilité
- ✅ Gestion automatique par Spring

---

## 📊 Récapitulatif des patterns

| Pattern | Usage | Bénéfice |
|---------|-------|----------|
| Service Layer | AuthService | Logique métier séparée |
| Filter | JwtAuthenticationFilter | Validation chaque requête |
| Strategy | JwtUtil | Algorithmes interchangeables |
| Builder | RefreshToken | Construction fluide |
| Decorator | @Slf4j, @Service | Responsabilités orthogonales |
| Template Method | SecurityConfig | Flux prévisible |
| Singleton | @Service | Une instance |
| Observer | ApplicationEventPublisher | Couplage faible |
| Factory | AuthenticationFactory | Création encapsulée |
| Immutable | AuthResponse | Thread-safe |
| Exception Handler | GlobalExceptionHandler | Gestion centralisée |
| Dependency Injection | @RequiredArgsConstructor | Testabilité |

---

**TokenGuard utilise les meilleurs patterns de conception!** 🏆
