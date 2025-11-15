# 🚀 Deployment Guide - TokenGuard

## 1. Architecture de Déploiement

### Environnements

```
┌──────────────────────────────────────────────────────────────┐
│                    PRODUCTION (LIVE)                         │
├──────────────────────────────────────────────────────────────┤
│  Load Balancer (Nginx)                                       │
│        ↓                                                      │
│  ┌─────────────┬─────────────┬─────────────┐               │
│  │  Instance 1 │  Instance 2 │  Instance 3 │               │
│  │  TokenGuard │  TokenGuard │  TokenGuard │               │
│  │  (Port 8080)│  (Port 8080)│  (Port 8080)│               │
│  └──────┬──────┴──────┬──────┴──────┬──────┘               │
│         └──────────────┬───────────────┘                     │
│                        ↓                                      │
│              PostgreSQL Cluster                              │
│              (Master + Replicas)                             │
│                                                              │
│              Redis Cache (Optional)                          │
│              (Token Blacklist)                               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    STAGING (PRE-PROD)                        │
├──────────────────────────────────────────────────────────────┤
│  Single Instance TokenGuard                                  │
│  PostgreSQL Single Instance                                  │
│  Same config as Prod (except scale)                          │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT (LOCAL)                       │
├──────────────────────────────────────────────────────────────┤
│  Spring Boot Dev Mode                                        │
│  H2 In-Memory Database                                       │
│  No Load Balancer                                            │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. Déploiement Local (Développement)

### 2.1 Prérequis

```bash
# Java 17+
java -version

# Maven 3.8+
mvn -version
```

### 2.2 Build et exécution

```bash
# Clone le repo
git clone https://github.com/georgesfk/tokenguard.git
cd tokenguard

# Build
mvn clean package

# Exécution
java -jar target/tokenguard-1.0.0.jar

# Ou avec Spring Boot Maven plugin
mvn spring-boot:run
```

### 2.3 Test de santé

```bash
curl http://localhost:8080/api/auth/health
# {"status":"UP"}
```

---

## 3. Déploiement Docker

### 3.1 Dockerfile multi-stage

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 as builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ src/
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le JAR du stage 1
COPY --from=builder /build/target/tokenguard-*.jar app.jar

# Santé check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/auth/health || exit 1

# Utilisateur non-root
RUN addgroup -g 1001 appuser && adduser -D -u 1001 -G appuser appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 Build et push Docker

```bash
# Build image
docker build -t tokenguard:1.0.0 .

# Tag pour registry
docker tag tokenguard:1.0.0 docker.io/georgesfk/tokenguard:1.0.0

# Login Docker Hub
docker login

# Push
docker push docker.io/georgesfk/tokenguard:1.0.0

# Tester localement
docker run -p 8080:8080 tokenguard:1.0.0
```

### 3.3 docker-compose pour dev

```yaml
version: '3.8'

services:
  tokenguard:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tokenguard
      SPRING_DATASOURCE_USERNAME: tokenguard
      SPRING_DATASOURCE_PASSWORD: secure_password_123
      JWT_SECRET: your_256_bit_secret_key_here_must_be_very_long
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/auth/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: tokenguard
      POSTGRES_USER: tokenguard
      POSTGRES_PASSWORD: secure_password_123
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tokenguard"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:  # Optional: For token blacklist caching
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

**Démarrer l'stack complète:**

```bash
docker-compose up -d

# Vérifier les logs
docker-compose logs -f tokenguard

# Arrêter
docker-compose down
```

---

## 4. Déploiement Kubernetes

### 4.1 ConfigMap (Secrets)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tokenguard-secrets
  namespace: production
type: Opaque
stringData:
  jwt-secret: "your_256_bit_secret_key_here_very_long_required_for_security"
  db-url: "jdbc:postgresql://postgres-service:5432/tokenguard"
  db-user: "tokenguard"
  db-password: "secure_password_123"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: tokenguard-config
  namespace: production
data:
  spring.profiles.active: "prod"
  logging.level.root: "INFO"
  logging.level.com.auth: "DEBUG"
```

### 4.2 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tokenguard
  namespace: production
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: tokenguard
      tier: api
  template:
    metadata:
      labels:
        app: tokenguard
        tier: api
    spec:
      containers:
      - name: tokenguard
        image: docker.io/georgesfk/tokenguard:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: tokenguard-config
              key: spring.profiles.active
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: tokenguard-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: tokenguard-secrets
              key: db-user
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: tokenguard-secrets
              key: db-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: tokenguard-secrets
              key: jwt-secret
        
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        
        livenessProbe:
          httpGet:
            path: /api/auth/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /api/auth/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        securityContext:
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
          runAsNonRoot: true
          runAsUser: 1001
          capabilities:
            drop:
            - ALL
```

### 4.3 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: tokenguard-service
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: tokenguard
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
    name: http
```

### 4.4 Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tokenguard-ingress
  namespace: production
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - auth.example.com
    secretName: tokenguard-tls
  rules:
  - host: auth.example.com
    http:
      paths:
      - path: /api/auth
        pathType: Prefix
        backend:
          service:
            name: tokenguard-service
            port:
              number: 80
```

### 4.5 Déployer sur K8s

```bash
# Créer le namespace
kubectl create namespace production

# Appliquer les configurations
kubectl apply -f secret.yaml
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# Vérifier le déploiement
kubectl get deployments -n production
kubectl get pods -n production
kubectl logs -f deployment/tokenguard -n production

# Port-forward pour test local
kubectl port-forward -n production service/tokenguard-service 8080:80
```

---

## 5. Déploiement Cloud (AWS/GCP/Azure)

### 5.1 AWS ECS (Elastic Container Service)

```json
{
  "family": "tokenguard",
  "taskRoleArn": "arn:aws:iam::ACCOUNT:role/tokenguard-task-role",
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/tokenguard-task-execution-role",
  "containerDefinitions": [
    {
      "name": "tokenguard",
      "image": "ACCOUNT.dkr.ecr.REGION.amazonaws.com/tokenguard:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:tokenguard/jwt-secret"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:tokenguard/db-url"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/tokenguard",
          "awslogs-region": "REGION",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/api/auth/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ],
  "cpu": "256",
  "memory": "512",
  "networkMode": "bridge",
  "requiresCompatibilities": ["EC2"],
  "cpu": "256",
  "memory": "512"
}
```

### 5.2 AWS RDS pour PostgreSQL

```bash
# Créer une instance RDS via AWS CLI
aws rds create-db-instance \
  --db-instance-identifier tokenguard-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username tokenguard \
  --master-user-password 'SecurePassword123!' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --publicly-accessible false \
  --multi-az true \
  --backup-retention-period 30
```

### 5.3 Déploiement AWS CodePipeline

```yaml
# buildspec.yml pour AWS CodeBuild
version: 0.2

phases:
  pre_build:
    commands:
      - echo Logging into Amazon ECR...
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
      - REPOSITORY_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/tokenguard
      - COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)
      - IMAGE_TAG=${COMMIT_HASH:=latest}

  build:
    commands:
      - echo Build started on `date`
      - mvn clean package -DskipTests
      - docker build -t $REPOSITORY_URI:$IMAGE_TAG .
      - docker tag $REPOSITORY_URI:$IMAGE_TAG $REPOSITORY_URI:latest

  post_build:
    commands:
      - echo Build completed on `date`
      - echo Pushing the Docker images...
      - docker push $REPOSITORY_URI:$IMAGE_TAG
      - docker push $REPOSITORY_URI:latest
      - echo Writing image definitions file...
      - printf '[{"name":"tokenguard","imageUri":"%s"}]' $REPOSITORY_URI:$IMAGE_TAG > imagedefinitions.json

artifacts:
  files: imagedefinitions.json

cache:
  paths:
    - '/root/.m2/**/*'
```

---

## 6. Base de Données

### 6.1 Initialisation PostgreSQL

```sql
-- Créer la base de données
CREATE DATABASE tokenguard;

-- Créer l'utilisateur
CREATE USER tokenguard WITH PASSWORD 'secure_password_123';

-- Accorder les permissions
GRANT ALL PRIVILEGES ON DATABASE tokenguard TO tokenguard;

-- Connexion à la BD
\c tokenguard;

-- Tables seront créées par Hibernate (spring.jpa.hibernate.ddl-auto=create)
-- Ou créer manuellement:

CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_refresh_token_username ON refresh_token(username);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
```

### 6.2 Sauvegarde et Restauration

```bash
# Sauvegarde
pg_dump -U tokenguard -h localhost tokenguard > backup.sql

# Sauvegarde compressée
pg_dump -U tokenguard -h localhost tokenguard | gzip > backup.sql.gz

# Restauration
psql -U tokenguard -h localhost tokenguard < backup.sql

# Restauration depuis compressé
gunzip -c backup.sql.gz | psql -U tokenguard -h localhost tokenguard
```

---

## 7. Monitoring et Logging

### 7.1 Configuration Logging

```yaml
# application-prod.yml
logging:
  level:
    root: INFO
    com.auth: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/tokenguard.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

### 7.2 Health Checks

```bash
# Endpoint de santé
curl http://localhost:8080/api/auth/health
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

### 7.3 Métriques avec Micrometer

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

```bash
# Accéder aux métriques
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

---

## 8. Checklist de Déploiement Production

- [ ] **Secrets Management**
  - [ ] JWT_SECRET (256+ bits)
  - [ ] Database credentials
  - [ ] API keys (si applicable)

- [ ] **Base de Données**
  - [ ] PostgreSQL cluster configuré
  - [ ] Backups automatiques activés
  - [ ] Indexes créés
  - [ ] Replication configurée

- [ ] **Application**
  - [ ] Tous les tests passent
  - [ ] Code review approuvée
  - [ ] Version taguée en Git
  - [ ] Docker image pushée

- [ ] **Infrastructure**
  - [ ] Load Balancer configuré
  - [ ] SSL/TLS certificate
  - [ ] Firewall rules
  - [ ] CDN (si applicable)

- [ ] **Monitoring**
  - [ ] Logging centralisé (ELK, Splunk)
  - [ ] Alertes configurées
  - [ ] Métriques Prometheus
  - [ ] APM (DataDog, New Relic)

- [ ] **Sécurité**
  - [ ] Scan des vulnérabilités
  - [ ] OWASP compliance
  - [ ] Rate limiting activé
  - [ ] CORS configuré

- [ ] **Documentation**
  - [ ] Runbooks créés
  - [ ] Procédures de rollback
  - [ ] Contact oncall
  - [ ] Documentation mise à jour

---

**TokenGuard prêt pour la production! 🎉**
