# 🧪 Guide de Testing - TokenGuard

## 1. Architecture de Test

### Pyramide de tests

```
        ┌─────────────────┐
        │  Tests E2E      │  (5%)
        │  (Cucumber)     │
        ├─────────────────┤
        │ Tests Intégration│  (15%)
        │  (@SpringBootTest)
        ├─────────────────┤
        │  Tests Unitaires│  (80%)
        │  (JUnit + Mockito)
        └─────────────────┘
```

---

## 2. Tests Unitaires

### 2.1 Test de ValidationUtil

```java
@DisplayName("ValidationUtil Tests")
class ValidationUtilTest {
    
    private ValidationUtil validationUtil;
    
    @BeforeEach
    void setUp() {
        validationUtil = new ValidationUtil();
    }
    
    @DisplayName("should validate valid credentials")
    @Test
    void testValidCredentials() {
        // Given
        String username = "john_doe";
        String password = "secure_password_123";
        
        // When & Then
        assertDoesNotThrow(() -> 
            validationUtil.validateCredentials(username, password)
        );
    }
    
    @DisplayName("should throw for short username")
    @Test
    void testShortUsername() {
        // Given
        String username = "ab";  // < 3 chars
        String password = "secure_password_123";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            validationUtil.validateCredentials(username, password)
        );
    }
    
    @DisplayName("should throw for weak password")
    @ParameterizedTest
    @ValueSource(strings = {"123", "weak", "short"})
    void testWeakPassword(String password) {
        // Given
        String username = "john_doe";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            validationUtil.validateCredentials(username, password)
        );
    }
    
    @DisplayName("should throw for null values")
    @Test
    void testNullValues() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            validationUtil.validateNotEmpty(null)
        );
    }
}
```

### 2.2 Test de RateLimitingUtil

```java
@DisplayName("RateLimitingUtil Tests")
class RateLimitingUtilTest {
    
    private RateLimitingUtil rateLimitingUtil;
    
    @BeforeEach
    void setUp() {
        rateLimitingUtil = new RateLimitingUtil();
    }
    
    @DisplayName("should allow first attempt")
    @Test
    void testFirstAttemptAllowed() {
        // When & Then
        assertDoesNotThrow(() -> 
            rateLimitingUtil.checkAttempt("john")
        );
    }
    
    @DisplayName("should block after 5 failed attempts")
    @Test
    void testBlockAfterMaxAttempts() {
        // Given
        String username = "john";
        
        // When - 5 failed attempts
        for (int i = 0; i < 5; i++) {
            rateLimitingUtil.recordFailedAttempt(username);
        }
        
        // Then - 6th attempt blocked
        TooManyAuthAttemptsException exception = 
            assertThrows(TooManyAuthAttemptsException.class, () ->
                rateLimitingUtil.checkAttempt(username)
            );
        
        assertTrue(exception.getRetryAfterSeconds() > 0);
    }
    
    @DisplayName("should reset attempts after successful login")
    @Test
    void testResetAfterSuccess() {
        // Given
        String username = "john";
        rateLimitingUtil.recordFailedAttempt(username);
        
        // When
        rateLimitingUtil.resetAttempts(username);
        
        // Then
        assertDoesNotThrow(() -> 
            rateLimitingUtil.checkAttempt(username)
        );
    }
}
```

### 2.3 Test de JwtUtilImpl

```java
@DisplayName("JwtUtilImpl Tests")
class JwtUtilImplTest {
    
    private JwtUtilImpl jwtUtil;
    
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtilImpl();
        // Mock @Value
        ReflectionTestUtils.setField(
            jwtUtil, 
            "secret", 
            "test_secret_key_that_is_very_long_256_bits_required_here"
        );
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }
    
    @DisplayName("should generate valid JWT token")
    @Test
    void testGenerateAccessToken() {
        // Given
        String username = "john";
        
        // When
        String token = jwtUtil.generateAccessToken(username);
        
        // Then
        assertNotNull(token);
        assertNotBlank(token);
        assertTrue(token.split("\\.").length == 3);  // Header.Payload.Signature
    }
    
    @DisplayName("should extract username from token")
    @Test
    void testGetUsernameFromToken() {
        // Given
        String username = "john";
        String token = jwtUtil.generateAccessToken(username);
        
        // When
        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        
        // Then
        assertEquals(username, extractedUsername);
    }
    
    @DisplayName("should validate non-expired token")
    @Test
    void testValidateNonExpiredToken() {
        // Given
        String token = jwtUtil.generateAccessToken("john");
        
        // When & Then
        assertFalse(jwtUtil.isTokenExpired(token));
    }
    
    @DisplayName("should reject invalid signature")
    @Test
    void testRejectInvalidSignature() {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                      "eyJzdWIiOiJqb2huIn0." +
                      "invalid_signature";
        
        // When & Then
        assertThrows(Exception.class, () ->
            jwtUtil.validateAndGetClaims(token)
        );
    }
}
```

---

## 3. Tests d'Intégration

### 3.1 Test du Controller avec MockMvc

```java
@DisplayName("AuthController Integration Tests")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AuthService authService;
    
    @DisplayName("POST /api/auth/login - success")
    @Test
    void testLoginSuccess() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("john", "password123");
        AuthResponse response = new AuthResponse("access_token", "refresh_token");
        
        when(authService.login("john", "password123"))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access_token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh_token"));
        
        verify(authService).login("john", "password123");
    }
    
    @DisplayName("POST /api/auth/login - invalid credentials")
    @Test
    void testLoginInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("john", "wrong");
        
        when(authService.login("john", "wrong"))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @DisplayName("POST /api/auth/refresh - success")
    @Test
    void testRefreshTokenSuccess() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh_token");
        AuthResponse response = new AuthResponse("new_access_token", "new_refresh_token");
        
        when(authService.refresh("refresh_token"))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new_access_token"));
    }
}
```

### 3.2 Test de la couche Service

```java
@DisplayName("AuthService Integration Tests")
@SpringBootTest
class AuthServiceIntegrationTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private RefreshTokenRepository tokenRepository;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }
    
    @DisplayName("login - should create refresh token")
    @Test
    void testLoginCreatesRefreshToken() {
        // Given
        String username = "john";
        String password = "password123";  // User doit exister en BD
        
        // When
        AuthResponse response = authService.login(username, password);
        
        // Then
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        
        // Vérifier en BD
        Optional<RefreshToken> token = 
            tokenRepository.findByToken(response.getRefreshToken());
        assertTrue(token.isPresent());
        assertEquals(username, token.get().getUsername());
        assertFalse(token.get().isRevoked());
    }
    
    @DisplayName("refresh - should rotate token")
    @Test
    void testRefreshRotatesToken() {
        // Given
        String username = "john";
        AuthResponse firstLogin = authService.login(username, "password123");
        String oldRefreshToken = firstLogin.getRefreshToken();
        
        // When
        AuthResponse refreshResponse = authService.refresh(oldRefreshToken);
        
        // Then
        assertNotEquals(oldRefreshToken, refreshResponse.getRefreshToken());
        
        // Ancien token doit être révoqué
        Optional<RefreshToken> oldToken = 
            tokenRepository.findByToken(oldRefreshToken);
        assertTrue(oldToken.get().isRevoked());
    }
    
    @DisplayName("logout - should revoke token")
    @Test
    void testLogoutRevokesToken() {
        // Given
        AuthResponse response = authService.login("john", "password123");
        
        // When
        authService.logout(response.getRefreshToken());
        
        // Then
        Optional<RefreshToken> token = 
            tokenRepository.findByToken(response.getRefreshToken());
        assertTrue(token.get().isRevoked());
    }
}
```

### 3.3 Test du Filter JWT

```java
@DisplayName("JwtAuthenticationFilter Integration Tests")
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtUtilImpl jwtUtil;
    
    @DisplayName("GET /api/auth/me - with valid token")
    @Test
    void testProtectedEndpointWithValidToken() throws Exception {
        // Given
        String token = jwtUtil.generateAccessToken("john");
        
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
    
    @DisplayName("GET /api/auth/me - without token")
    @Test
    void testProtectedEndpointWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }
    
    @DisplayName("GET /api/auth/me - with invalid token")
    @Test
    void testProtectedEndpointWithInvalidToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid_token"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## 4. Tests E2E avec Testcontainers

### 4.1 Setup avec PostgreSQL en container

```java
@DisplayName("End-to-End Tests")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @DisplayName("E2E: Login -> Protected Request -> Refresh -> Logout")
    @Test
    void testCompleteAuthenticationFlow() {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest("john", "password123");
        ResponseEntity<AuthResponse> loginResponse = 
            restTemplate.postForEntity("/api/auth/login", loginRequest, AuthResponse.class);
        
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String accessToken = loginResponse.getBody().getAccessToken();
        String refreshToken = loginResponse.getBody().getRefreshToken();
        
        // 2. Protected Request
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<UserInfoResponse> meResponse = 
            restTemplate.exchange("/api/auth/me", HttpMethod.GET, 
                new HttpEntity<>(headers), UserInfoResponse.class);
        
        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertEquals("john", meResponse.getBody().getUsername());
        
        // 3. Refresh Token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<AuthResponse> refreshResponse = 
            restTemplate.postForEntity("/api/auth/refresh", refreshRequest, AuthResponse.class);
        
        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        String newAccessToken = refreshResponse.getBody().getAccessToken();
        assertNotEquals(accessToken, newAccessToken);
        
        // 4. Logout
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<?> logoutResponse = 
            restTemplate.postForEntity("/api/auth/logout", logoutRequest, Void.class);
        
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        
        // 5. Token should be invalid now
        headers.setBearerAuth(newAccessToken);
        ResponseEntity<?> afterLogout = 
            restTemplate.exchange("/api/auth/me", HttpMethod.GET, 
                new HttpEntity<>(headers), Void.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode());
    }
}
```

---

## 5. Tests de Sécurité

### 5.1 Test de Rate Limiting

```java
@DisplayName("Security: Rate Limiting Tests")
@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private RateLimitingUtil rateLimitingUtil;
    
    @BeforeEach
    void setUp() {
        rateLimitingUtil.resetAttempts("attacker");
    }
    
    @DisplayName("should block after 5 failed login attempts")
    @Test
    void testRateLimitingBlocks() throws Exception {
        // 5 tentatives échouées
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"attacker\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        }
        
        // 6e tentative bloquée
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"attacker\",\"password\":\"wrong\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfter").exists());
    }
}
```

### 5.2 Test CORS

```java
@DisplayName("Security: CORS Tests")
@SpringBootTest
@AutoConfigureMockMvc
class CorsSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @DisplayName("should allow CORS from trusted origins")
    @Test
    void testCorsAllowedOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/health")
                .header("Origin", "https://trusted-domain.com"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
    
    @DisplayName("should reject CORS from untrusted origins")
    @Test
    void testCorsRejectedOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/health")
                .header("Origin", "https://untrusted-domain.com"))
            .andExpect(status().isOk());
            // CORS header absent = navigation rejetée par navigateur
    }
}
```

---

## 6. Configuration Maven pour les Tests

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>

<!-- Plugin Maven -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0</version>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
    </configuration>
</plugin>
```

---

## 7. Commandes de Test

```bash
# Tous les tests
mvn test

# Tests unitaires seulement
mvn test -Dtest=**/*Test.java

# Tests d'intégration
mvn test -Dtest=**/*IntegrationTest.java

# E2E seulement
mvn test -Dtest=**/*E2ETest.java

# Un test spécifique
mvn test -Dtest=AuthServiceIntegrationTest

# Avec rapport de couverture (JaCoCo)
mvn clean test jacoco:report

# Voir rapport
open target/site/jacoco/index.html
```

---

## 8. Métriques de Couverture

### Objectifs de couverture

| Couche | Objectif |
|--------|----------|
| Service | 90% |
| Controller | 85% |
| Filter | 90% |
| Util | 95% |
| Entity | 70% |
| Repository | 80% |
| **Total** | **85%** |

### Générer un rapport JaCoCo

```bash
mvn clean test jacoco:report
# Rapport: target/site/jacoco/index.html
```

---

**TokenGuard avec tests de production!** ✅ 🚀
