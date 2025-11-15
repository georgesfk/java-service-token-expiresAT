# 🤝 Contributing to TokenGuard

Merci de vouloir contribuer à **TokenGuard**! 

## 📋 Comment contribuer

### 🐛 Signaler un bug

1. Ouvrir une [issue](https://github.com/georgesfk/TokenGuard/issues)
2. Décrire le bug clairement
3. Fournir les étapes pour reproduire
4. Indiquer votre environnement (Java, Spring Boot, OS)

**Exemple:**
```
Titre: JWT filter ne valide pas les tokens expirés

Description:
- Java 17
- Spring Boot 3.1.5
- Les tokens expirés sont acceptés au lieu d'être rejetés

Steps to reproduce:
1. Login
2. Attendre 1+ heure
3. Faire une requête avec le token
```

### ✨ Suggérer une feature

1. Ouvrir une [issue](https://github.com/georgesfk/TokenGuard/issues)
2. Décrire la feature
3. Expliquer le use case
4. Suggérer l'implémentation si possible

### 💻 Contribuer du code

#### 1. Fork le repository

```bash
git clone https://github.com/votre-username/TokenGuard.git
cd TokenGuard
git checkout -b feature/nom-feature
```

#### 2. Suivre le style de code

- Convention Java standard (CamelCase)
- Indentation: 4 espaces
- Ajouter `@Slf4j` pour le logging
- Documenter avec Javadoc
- Ajouter des tests unitaires

#### 3. Commit et push

```bash
git add .
git commit -m "feat: Description claire de la feature"
git push origin feature/nom-feature
```

#### 4. Créer une Pull Request

1. Aller sur GitHub
2. Cliquer sur "Compare & pull request"
3. Décrire les changements
4. Attendre la review

---

## 📝 Style de commit

Suivre le format [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat` - Nouvelle feature
- `fix` - Correction de bug
- `docs` - Documentation
- `style` - Format de code
- `refactor` - Refactorisation
- `perf` - Performance
- `test` - Tests
- `chore` - Maintenance

### Exemples

```
feat(auth): ajouter 2FA support

- Intégrer TOTP
- Valider les codes OTP
- Ajouter les tests

Closes #123
```

```
fix(jwt): corriger la validation des tokens expirés

Le filtre acceptait les tokens expirés.
```

---

## 🧪 Tests

Tous les PRs doivent inclure des tests:

```bash
mvn clean test
```

Avant de soumettre:
- ✅ Tous les tests passent
- ✅ Code formaté
- ✅ Documentation à jour
- ✅ Commit messages clairs

---

## 📚 Documentation

Pour toute nouvelle feature:
1. Mettre à jour le README.md
2. Ajouter la javadoc
3. Ajouter des exemples curl
4. Mettre à jour IMPLEMENTATION_GUIDE.md

---

## 🔍 Code Review

Un maintainer va:
1. Vérifier le code
2. Tester localement
3. Laisser des commentaires
4. Valider ou demander des changements

---

## 📦 Release

Les releases suivent [Semantic Versioning](https://semver.org/):
- `MAJOR.MINOR.PATCH`
- Exemple: `1.2.3`

---

## ❓ Questions?

- Ouvrir une [discussion](https://github.com/georgesfk/TokenGuard/discussions)
- Envoyer un email à: [@georgesfk](https://github.com/georgesfk)

---

**Merci de contribuer à TokenGuard!** 🙏
