# AGENTS.md — UMSS Market

> Archivo de configuración de agentes IA para el repositorio UMSS Market.
> Sincronizado con `docs/DTI.md` — cualquier cambio arquitectónico significativo debe actualizar ambos archivos en el mismo commit.
>
> **Regla de oro**: los agentes que lean este archivo tienen permiso de actuar sobre el código y la documentación de este repositorio únicamente dentro de los límites declarados aquí.

---

## Producto

**Nombre**: UMSS Market  
**Grupo**: G1  
**Release activo**: `release/2.0.0`  
**DTI**: `docs/DTI.md`  
**FSD**: `docs/FSD_v2.md`  
**Autores**: Rodriguez Gonzales Abad Melani, Vargas Sandoval Christian Bernardo

---

## Stack

```yaml
backend:
  runtime: Python 3.12
  framework: FastAPI
  orm: SQLAlchemy 2.0
  async: asyncio + aio-pika

frontend:
  framework: React 18
  type: PWA (mobile-first)
  state: Zustand

database:
  primary: PostgreSQL 16
  cache: Redis 7

messaging:
  broker: RabbitMQ 3.13
  pattern: Event-Driven (publish/subscribe)

infrastructure:
  cloud: AWS
  compute: ECS Fargate
  ci_cd: GitHub Actions + ECR

testing:
  unit: pytest
  load: k6
  contract: tests/guardrails/
```

---

## Estructura del repositorio

```
/
├── AGENTS.md                    ← este archivo
├── docs/
│   ├── DTI.md                   ← contrato técnico principal
│   ├── fsd/FSD_vFinal.md                ← especificación funcional
│   ├── prd/PRD_vFinal.md                ← requerimientos de producto
│   ├── mrd/MRD_vFinal.md                ← requerimientos de mercado
│   ├── brd/BRD_vFinal.md                ← requerimientos de negocio
│   ├── PROMPT_MAPPINGS_v1.md   ← trazabilidad de prompts
│   ├── PR-FSD-001.md            ← contrato IA: validación pago QR
│   ├── PR-FSD-002.md            ← contrato IA: validación stock
│   ├── PR-FSD-003.md            ← contrato IA: eventos distribuidos
│   ├── adr/
│   │   ├── ADR-0001-event-driven-architecture.md
│   │   ├── ADR-0002-saga-pattern.md
│   │   └── ADR-0003-hexagonal-architecture.md
│   ├── architecture/
│   │   ├── BOUNDED_CONTEXTS.md
│   │   ├── EVENT_CATALOG.md
│   │   ├── EVENT_DRIVEN.md
│   │   ├── ASYNC_PATTERNS.md
│   │   └── CORE_DOMAIN.md
│   └── plantillas/
├── diagrams/
│   ├── c4-context.mmd
│   ├── c4-container.mmd
│   └── event-flow.mmd
├── poc/
│   ├── umss_ecommerce.py       ← POC-01: lógica core CLI
│   └── umss_ecommerce.html     ← POC-02: flujo UX QR
├── prompts/
│   ├── PRD_PROMPT.md
│   ├── FSD_PROMPT.md
│   └── ADR_PROMPT.md
└── skills/
    ├── distributed_architecture_reviewer_SKILL.md
    ├── event_catalog_author_SKILL.md
    └── saga_designer_SKILL.md
```

---

## Capas arquitectónicas

```
presentation/        → React 18 PWA (adapter/in/web)
api_gateway/         → FastAPI (auth JWT, rate limiting, routing)
services/            → 6 microservicios Python/FastAPI
  order-service/     → gestión del ciclo de vida de pedidos + Saga coordinator
  payment-service/   → QR dinámico + webhook bancario + idempotencia
  inventory-service/ → stock atómico + reservas Redis + liberación
  catalog-service/   → productos, tiendas, categorías (multi-tenant)
  notification-service/ → push FCM + email
  realtime-gateway/  → WebSocket actualizaciones live
domain/              → aggregates, entities, value objects, ports (hexagonal core)
infrastructure/      → adapters: PostgreSQL, RabbitMQ, Redis, API Bancaria, SIIS
```

---

## Invariantes del dominio (no negociables)

Los agentes NUNCA deben generar código que viole estas reglas:

1. **Stock no negativo**: `Product.stock >= 0` en todo momento. La operación de descuento debe ser atómica: `UPDATE products SET stock = stock - cantidad WHERE id = ? AND stock >= cantidad`.
2. **Idempotencia de pagos**: Un `webhook_ref` solo puede procesarse una vez. Índice único en la columna `webhook_ref` de la tabla `payments`. Si llega duplicado → HTTP 200 sin reprocesar.
3. **QR con TTL**: Cada QR expira en exactamente 300 segundos (5 minutos) desde su creación. Al expirar → pedido a CANCELADO + stock liberado automáticamente.
4. **RU obligatorio**: Ningún usuario puede registrarse sin un RU activo validado contra SIIS UMSS. Sin excepción.
5. **Monto exacto**: El monto del webhook bancario debe coincidir exactamente con `Pedido.total`. Cualquier diferencia → rechazo.
6. **Secretos fuera del código**: Ningún secret, API key, contraseña o token debe aparecer hardcodeado en ningún archivo del repositorio. Usar variables de entorno / AWS Secrets Manager.
7. **HMAC en webhooks**: Todo webhook bancario entrante debe validar su firma HMAC-SHA256 antes de procesar el payload.

---

## Eventos del sistema

| Evento | Productor | Consumidor(es) |
|--------|-----------|----------------|
| `ORDER_CREATED` | order-service | payment-service |
| `PAYMENT_PENDING` | payment-service | order-service |
| `PAYMENT_CONFIRMED` | payment-service | inventory-service |
| `PAYMENT_FAILED` | payment-service | order-service |
| `STOCK_RESERVED` | inventory-service | order-service |
| `STOCK_RELEASED` | inventory-service | order-service |
| `ORDER_CONFIRMED` | order-service | notification-service, realtime-gateway |
| `ORDER_CANCELLED` | order-service | notification-service |

> Referencia completa: `docs/architecture/EVENT_CATALOG.md`

---

## ADRs vigentes

| ADR | Título | Estado |
|-----|--------|--------|
| [ADR-0001](docs/adr/ADR-0001-event-driven-architecture.md) | Arquitectura Orientada a Eventos | Aceptada |
| [ADR-0002](docs/adr/ADR-0002-saga-pattern.md) | Saga Pattern (Coreografía) | Propuesta |
| [ADR-0003](docs/adr/ADR-0003-hexagonal-architecture.md) | Arquitectura Hexagonal / Clean | Aceptada |

---

## Contratos funcionales IA

Los agentes que generen o validen código relacionado con los flujos críticos deben respetar los contratos:

| Contrato | Flujo cubierto | Ubicación |
|----------|---------------|-----------|
| `PR-FSD-001` | Validación de confirmación de pago QR (webhook) | `docs/PR-FSD-001.md` |
| `PR-FSD-002` | Validación de disponibilidad y reserva de stock | `docs/PR-FSD-002.md` |
| `PR-FSD-003` | Coordinación de eventos distribuidos (ORDER_CREATED, etc.) | `docs/PR-FSD-003.md` |

---

## Skills disponibles

| Skill | Propósito | Activación |
|-------|-----------|------------|
| `dti-author` | Poblar secciones del DTI; sincronizar con AGENTS.md | `@dti-author §N <tema>` |
| `c4-architect` | Generar diagramas C4 en Mermaid | `@c4-architect nivel <1/2/3>` |
| `poc-runner` | Scaffold y validación de POCs | `@poc-runner POC-NN` |
| `distributed_architecture_reviewer` | Revisar decisiones de arquitectura distribuida | `skills/distributed_architecture_reviewer_SKILL.md` |
| `event_catalog_author` | Crear y actualizar el catálogo de eventos | `skills/event_catalog_author_SKILL.md` |
| `saga_designer` | Diseñar flujos Saga con compensaciones | `skills/saga_designer_SKILL.md` |

---

## Restricciones para agentes

### Permitido
- Leer cualquier archivo del repositorio
- Generar código en `backend/`, `frontend/`, `tests/`
- Crear y editar archivos en `docs/`, `diagrams/`, `poc/`
- Proponer cambios en `docs/adr/` con nuevo ADR
- Actualizar `docs/DTI.md` y `AGENTS.md` en el mismo commit cuando hay cambio arquitectónico

### Requiere confirmación humana explícita
- Eliminar archivos del repositorio
- Modificar `docs/adr/*.md` ya existentes (estado Aceptada)
- Cambiar los invariantes del dominio declarados en este archivo
- Cualquier operación que afecte datos de producción o usuarios reales
- Push a `main` o `release/*`
- Cambios en configuración de AWS (IAM, Security Groups, RDS)

### Prohibido
- Hardcodear secretos, API keys, passwords o tokens en cualquier archivo
- Eliminar validaciones de HMAC o idempotencia de pagos
- Reducir el cost de bcrypt por debajo de 12
- Generar código que permita `stock < 0`
- Ignorar el campo `correlationId` en eventos del sistema
- Generar código de producción sin tests asociados que cubran al menos el **90%** de las líneas del feature

---

## Reglas de calidad de código

| Regla | Valor mínimo | Herramienta | Scope |
|-------|-------------|-------------|-------|
| Cobertura de líneas (line coverage) | **90%** | JaCoCo (Java), pytest-cov (Python) | Por feature implementado |
| Cobertura de ramas (branch coverage) | **80%** | JaCoCo | Por feature implementado |

Los agentes deben generar tests unitarios y de integración para **todo código nuevo** de forma que JaCoCo reporte ≥ 90% de cobertura de líneas sobre las clases del feature. Si el reporte de cobertura no alcanza el umbral, el agente debe agregar casos de prueba adicionales antes de considerar el feature completo.

---

## Ciclo de commits sincronizados

Cuando un agente realiza un cambio que afecta una decisión arquitectónica:

```
docs(dti+agents): <descripción de la decisión> [ADR-NNNN]
```

Ejemplo:
```
docs(dti+agents): adoptar Redis TTL para bloqueo de stock [ADR-0002]
```

El DTI (`docs/DTI.md`) y el AGENTS.md deben actualizarse en el **mismo commit**.

---

## Sincronía con DTI

| Sección AGENTS.md | Sección DTI correspondiente |
|-------------------|-----------------------------|
| Stack | §0 frontmatter YAML |
| Capas arquitectónicas | §3.2 C4 Nivel 2 |
| Invariantes del dominio | §4.2 Entidades y Aggregates + §13 Seguridad |
| Eventos del sistema | §7.1 Catálogo de eventos |
| ADRs vigentes | §21 Registro de ADRs |
| Contratos funcionales IA | §9.2 + §10 Prompt Mapping |
| Skills disponibles | §0.1 Rol de agentes en SDLC |
| Restricciones | §13 Seguridad + §22 Auditoría IA |

---

## Registro de cambios

| Versión | Fecha | Autor | Cambio |
|---------|-------|-------|--------|
| v1.0 | 25/05/2026 | Rodriguez / Vargas | Creación inicial — sincronizado con DTI v2.0 |

## Demostración académica de compra — Angular / Spring Boot (2026-09-15)

El flujo de demostración usa `POST /api/orders/checkout/simulated`.
`SimulatedCheckoutUseCase` valida usuario, publicaciones y cantidades, calcula los
importes desde persistencia y guarda pedido CONFIRMADO, ítems y descuento de stock
en una transacción local. El puerto `CheckoutRepositoryPort` mantiene las operaciones
de bloqueo y persistencia fuera del caso de uso; su adaptador JPA aplica un UPDATE
de stock con guarda `stock >= quantity`. Bloquea publicaciones en orden estable y
serializa compras del mismo usuario para recuperar reintentos con el mismo
`requestId`, utilizado como ID del pedido. Un ID reutilizado con otro contenido
se rechaza. La inserción no sobrescribe un pedido existente.

Este endpoint representa pago simulado inmediato para la tarea académica; no
implementa el flujo bancario QR, sus webhooks, TTL ni eventos distribuidos. Los
contratos de esos flujos permanecen vigentes. El frontend usa un usuario de prueba
existente configurado en `src/app/core/config/demo.ts`; la integración de login
no forma parte de esta demo. Las rutas CRUD anteriores conservan su comportamiento.

Validación: `SimulatedCheckoutTest` usa H2 aislada para probar persistencia,
rollback, stock insuficiente, reintentos y compras concurrentes. Ejecutar
`./mvnw.cmd -Dtest=SimulatedCheckoutTest test jacoco:report` desde el backend.
