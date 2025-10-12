
# Smart Grid — Backend (Java / Vert.x)

Ce dépôt contient le backend du projet Smart Grid : un service Java (Vert.x + JPA) qui collecte des mesures issues de capteurs/simulateurs (panneaux solaires, éoliennes, chargeurs EV, etc.) et expose des routes HTTP/UDP pour le frontend et les simulateurs.

Petit et clair — ce README vise à présenter le projet et fournir les commandes essentielles pour démarrer en local.

## Stack technique
- Langage : Java (JDK 11+ recommandé)
- Frameworks : Vert.x pour le serveur asynchrone, JPA pour la persistence
- Base de données : PostgreSQL (via Docker Compose)
- Orchestration locale : Docker Compose
- Build & exécution : Gradle Wrapper (`./gradlew`)

## Démarrage rapide
Ouvrez un terminal à la racine du dépôt (là où se trouve `docker-compose.yml` et `gradlew`) puis :

```bash
# Démarrer la stack (Postgres, Adminer, frontend, simulateurs)
docker compose up -d

# Lancer le backend (en local, depuis la racine)
./gradlew run

# Build
./gradlew build

# Tests
./gradlew test
```

Sous Windows, utilisez `./gradlew.bat`.

Ports utiles (configurés dans `docker-compose.yml`) :
- Backend : 8080
- Adminer : 8081
- Frontend (si fourni) : 8082

## Structure importante

- `docker-compose.yml` — stack locale (Postgres, Adminer, frontend, simulateurs)
- `init_database.sql` — script d'initialisation de la BDD
- `build.gradle`, `settings.gradle`, `gradlew*` — Gradle et wrapper
- `src/main/java/fr/imta/smartgrid` — code source Java
	- `model` — entités JPA (Consumer, Producer, Sensor, Measurement, Grid, Person...)
	- `server` — démarrage (`VertxServer`), handlers HTTP et UDP
- `src/main/resources/META-INF/persistence.xml` — config JPA / datasource

## API & routes
La spécification des routes (ingress capteurs, API REST pour persons/grids/sensors/measurements...) se trouve dans `smart_grid/backend_routes.md`. Ce document donne des exemples de payloads et de réponses attendues.

## Développement
- Lancer localement : `./gradlew run` ou exécuter `fr.imta.smartgrid.server.VertxServer` depuis votre IDE (IntelliJ/Eclipse).
- Pour réinitialiser la base :

```bash
docker compose down --volumes && docker compose up -d
```


