# openfactory-api

REST API del proyecto **openFactory** — convierte ideas en workpacks estructurados con briefs, boxes de ejecución y planes generados por IA.

**Stack:** Java 21 · Quarkus 3.8.1 · PostgreSQL (Supabase) · JWT Bearer (Supabase ECC P-256)
**Dependencia core:** `openfactory-core-lib 0.1.0-alpha`

---

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/workpacks/ingest` | Crea workpack y corre el pipeline async |
| `GET`  | `/api/workpacks` | Lista workpacks del usuario (propios + compartidos) |
| `GET`  | `/api/workpacks/{id}` | Obtiene un workpack (polling de processingStatus) |
| `GET`  | `/api/workpacks/{id}/export` | Vista completa: brief, boxes, plan, handoff |
| `POST` | `/api/workpacks/{id}/shape` | Re-corre el pipeline sobre contenido existente |
| `POST` | `/api/workpacks/{id}/advance` | Avanza de stage (RAW → DEFINE → SHAPE → BOX) |
| `GET`  | `/api/workpacks/{id}/brief` | Brief generado |
| `GET`  | `/api/workpacks/{id}/boxes` | Boxes de trabajo |
| `GET`  | `/api/workpacks/{id}/plan` | Plan de ejecución |
| `GET`  | `/api/workpacks/{id}/handoff` | Handoff package |
| `POST` | `/api/workpacks/{id}/members` | Invitar colaborador por email |
| `GET`  | `/api/me` | Perfil del usuario autenticado |
| `GET`  | `/api/inbox` | Notificaciones del usuario |

## Pipeline async

`POST /ingest` retorna inmediatamente con `processingStatus: PROCESSING`.
El cliente hace polling a `GET /api/workpacks/{id}` hasta `processingStatus: DONE`.

```
content → SessionIngestionService → BriefBuilder → OutlineService
       → BoxGenerator → ExecutionPlanner → HandoffService → DB
```

---

## Correr en dev

Requiere variables de entorno:

```bash
ANTHROPIC_API_KEY=sk-ant-...
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://...
QUARKUS_DATASOURCE_USERNAME=...
QUARKUS_DATASOURCE_PASSWORD=...
```

```bash
./mvnw compile quarkus:dev
```

En modo dev el AuthFilter usa un usuario de prueba fijo (sin JWT real).

## Tests

```bash
./mvnw test
```

Usa H2 in-memory + auth bypass. No requiere infraestructura externa.

## Migrations

Flyway corre automáticamente al iniciar. Archivos en `src/main/resources/db/migration/`.

---

## Estado del sprint API

| Ticket | Descripción | Estado |
|--------|-------------|--------|
| API-001 | CORS extension | Pendiente |
| API-002 | AuthFilter JWT Supabase | ✅ |
| API-003 | UserResource | ✅ |
| API-004 | PinResource | ✅ |
| API-005 | BriefResource | ✅ |
| API-006 | BoxResource | ✅ |
| API-007 | PlanResource | ✅ |
| API-008 | HandoffResource | ✅ |
| API-009 | InboxResource | ✅ |
| API-010 | Colaboración / Members | ✅ |
| API-011 | Pipeline Orchestrator | ✅ |
| API-012 | Package download ZIP | Pendiente |
