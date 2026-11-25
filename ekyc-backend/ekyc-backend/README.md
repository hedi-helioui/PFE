# E-KYC : backend (MVP)

Backend Spring Boot de la plateforme E-KYC : enrôlement client (selfie, vidéo, document), pipeline de vérification
(OCR, cohérence, fraude documentaire, face match, liveness) et back-office de validation manuelle.
Les services IA sont **mockés** derrière des interfaces ; ils seront remplacés par le service Python.

## Prérequis

- Java 17 ou plus, Maven 3.9+
- Docker (pour PostgreSQL) **ou** un PostgreSQL local

## Lancer

```bash
# 1. Base de données (PostgreSQL 16 dans Docker, exposé sur le port 5433)
docker compose up -d

# 2. Application
mvn spring-boot:run
```

- Swagger UI : http://localhost:8080/swagger-ui.html
- Comptes de démonstration (créés au démarrage) : `agent@kyc.local` / `Agent123!` et `admin@kyc.local` / `Admin123!`
  (à désactiver en production : `kyc.seed.enabled=false`)

**Si tu préfères ton PostgreSQL local** : crée une base `ekyc` puis lance avec
`DB_URL=jdbc:postgresql://localhost:5432/ekyc DB_USER=... DB_PASSWORD=... mvn spring-boot:run`
(ou modifie `application.yml`).

## Tests

```bash
mvn test
```

Les tests utilisent H2 en mémoire : pas besoin de Docker.

## Parcours de démonstration (curl)

```bash
# Inscription d'un client (renvoie un jeton JWT)
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"client@test.com","password":"Password123!","fullName":"Mohamed Ben Ali"}'
TOKEN=<le token reçu>

# Ouvrir un dossier avec l'identité déclarée
curl -s -X POST localhost:8080/api/v1/kyc/cases -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"declaredFullName":"Mohamed Ben Ali","declaredIdNumber":"12345678","declaredBirthDate":"1998-05-14"}'

# Envoyer les pièces (remplace 1 par l'id du dossier)
curl -s -X POST localhost:8080/api/v1/kyc/cases/1/selfie   -H "Authorization: Bearer $TOKEN" -F "file=@selfie.jpg;type=image/jpeg"
curl -s -X POST localhost:8080/api/v1/kyc/cases/1/video    -H "Authorization: Bearer $TOKEN" -F "file=@video.mp4;type=video/mp4"
curl -s -X POST "localhost:8080/api/v1/kyc/cases/1/document?type=CIN" -H "Authorization: Bearer $TOKEN" -F "file=@cin.png;type=image/png"

# Soumettre : lance la vérification et renvoie le statut
curl -s -X POST localhost:8080/api/v1/kyc/cases/1/submit -H "Authorization: Bearer $TOKEN"
```

## Endpoints

| Domaine | Endpoints | Rôle |
|---|---|---|
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` | public |
| Client | `POST /api/v1/kyc/cases`, `GET /api/v1/kyc/cases`, `GET /{id}`, `POST /{id}/selfie`, `/{id}/video`, `/{id}/document?type=CIN\|PASSPORT`, `/{id}/submit` | CLIENT |
| Back-office | `GET /api/v1/backoffice/cases?status=`, `GET /{id}`, `POST /{id}/decision`, `GET /{id}/audit`, `GET /{id}/documents/{docId}/file`, `GET /{id}/media/{mediaId}/file` | AGENT, ADMIN |

## Logique de décision

Après `submit`, le pipeline calcule 4 scores puis décide (seuils dans `application.yml`, section `kyc.thresholds`) :

- `REJECTED` : face match, liveness ou cohérence < `reject` (0.50), ou fraude >= `fraud-reject-min` (0.60)
- `APPROVED` : face match, liveness et cohérence >= `approve` (0.80) et fraude <= `fraud-approve-max` (0.20)
- sinon `MANUAL_REVIEW` : un agent tranche via `POST /api/v1/backoffice/cases/{id}/decision`
  (`{"decision":"APPROVE"|"REJECT","comment":"..."}`, commentaire obligatoire pour rejeter)

Le score de cohérence compare les données lues par l'OCR (nom, numéro, date de naissance) au formulaire ; c'est la seule
vérification réelle pour l'instant, les trois autres scores viennent des mocks.

Pour simuler un cas, change les valeurs de `kyc.mock.*` dans `application.yml`
(ex. `face-match-score: 0.65` envoie le dossier en revue manuelle).

## Brancher le vrai service IA

Les interfaces sont dans `com.pfe.ekyc.ai` : `FaceMatchService`, `LivenessService`, `OcrService`, `FraudDetectionService`.
Les mocks (`ai/mock`) sont actifs quand `kyc.ai.mode=mock`. Pour passer au service Python (FastAPI) :
crée des implémentations qui appellent son API HTTP, annote-les avec
`@ConditionalOnProperty(name = "kyc.ai.mode", havingValue = "http")` et mets `kyc.ai.mode: http`.
Le reste du backend ne change pas.

## Structure

```
src/main/java/com/pfe/ekyc
├── ai/          interfaces IA + mocks
├── config/      propriétés, OpenAPI, comptes de démonstration
├── controller/  endpoints REST
├── domain/      entités JPA (User, KycCase, KycDocument, KycMedia, VerificationResult, AuditLog)
├── dto/         objets d'entrée/sortie
├── exception/   erreurs métier et gestionnaire global (ProblemDetail)
├── repository/  Spring Data JPA
├── security/    JWT, filtre, règles d'accès
├── service/     logique métier, pipeline de vérification, audit
└── storage/     stockage des fichiers (disque local)
```

## Hors périmètre du MVP (perspectives)

Vraie intégration IA, chiffrement des fichiers au repos, webhooks vers la banque, traitement asynchrone du pipeline,
limitation de débit, migrations Flyway (le schéma est actuellement géré par `ddl-auto: update`), tests de charge.

## Sécurité : à changer avant toute mise en production

- `JWT_SECRET` (32 caractères minimum) : la valeur par défaut est réservée au développement
- comptes de démonstration (`kyc.seed.enabled=false`)
- identifiants PostgreSQL de `docker-compose.yml`
