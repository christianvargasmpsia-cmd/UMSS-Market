# Plan de implementación de salud

## Summary

- Builds: verificar el endpoint existente y completar pruebas de recuperación y éxito JDBC.
- Touches: pruebas HealthDatabaseProbeTest, documentación de salud y registros de esta etapa; código productivo solo si incumple requisitos.
- Tests: seis pruebas existentes más éxito JDBC y recuperación tras fallo; baseline completo y validación del feature.

## Pasos y trazabilidad

- [x] Step 1: verificar Java 21/Maven y configuración de pruebas; ejecutar suite existente antes de cambios y registrar baseline (NFR4, NFR5).
- [x] Step 2: revisar HealthDatabaseProbe frente a FR2, FR4, FR5 y NFR3; conservar implementación si cumple, completar prueba feliz y recuperación tras conexión fallida, ejecutar pruebas JDBC.
- [x] Step 3: revisar HealthController y sus pruebas frente a FR1, FR3, FR4, NFR1 y NFR2; conservar contrato y validar HTTP e integración H2.
- [x] Step 4: ejecutar pruebas del feature y JaCoCo, verificar mínimo 90% líneas y 80% ramas; comparar suite completa tras cambios con baseline (NFR4, NFR5).
- [x] Step 5: actualizar documentación si cambia el inventario de pruebas y producir code-summary.md y traceability.json para FR1..FR5 y NFR1..NFR5.

## Alcance y seguridad

Sin migraciones, frontend ni cambios de arquitectura. No alterar SecurityConfig, bcrypt ni funcionalidades comerciales. No usar bases externas ni desplegar. No commit/push automático. Fallos preexistentes se documentan sin ampliar el feature. No crear Unit/Bolt; alcance express sin unidades. No bajar umbrales de cobertura.

## Impacto previsto

HealthDatabaseProbeTest.java es el cambio esperado: añadir casos sobre la sonda existente. docs/HEALTH_ENDPOINT.md puede reflejar el inventario. HealthController y HealthDatabaseProbe se conservan salvo defecto demostrado. Test-after: la implementación ya existe; completar pruebas sobre esa implementación. El baseline registra su estado actual, no reconstruye un estado previo inexistente.

## Testing Contract

```json
{
  "version": 1,
  "methodology": "test-after",
  "source": "org",
  "ordering": "implement each applicable testable layer, then write and run that layer's tests.",
  "scope": "express",
  "test_strategy": "minimal",
  "project_type": "brownfield",
  "applicable_notes": [
    {
      "layer": "org",
      "text": "We treat tests as a first-class deliverable in every Bolt. The specific\nmethodology (TDD, BDD, ATDD, or classic test-after) is affirmed at\npractices-discovery and recorded in `team.md` under this heading with explicit\n`Methodology` and `Ordering` fields; Code Generation resolves those fields\nindependently from coverage, tooling, and scope notes.\n\nWhen no posture has been affirmed, our default per scope is:\n- **Methodology**: test-after\n- **Ordering**: implement each applicable testable layer, then write and run\n  that layer's tests.\n- `mvp`, `enterprise`, `feature`, `infra`, `classic` add an 80% line-coverage\n  floor and CI execution before merge.\n- `bugfix`, `security-patch` add a targeted regression for the specific\n  bug/vulnerability and require the existing suite to remain green.\n- `express` uses the Minimal strategy: requirement-driven unit tests (one per\n  requirement, with a happy-path floor per component); existing tests remain\n  green.\n- `poc`, `refactor`, `workshop` add no extra new-test floor and require the\n  existing suite to remain green.\n\nThe active `Test Strategy` still applies in every scope and determines test\nvolume/types. Scope floors are additive; they never reduce or replace the\nselected strategy.\n\nBuild and Test verifies defined coverage floors and affirmed quality targets;\nthey may not be weakened to make a step pass.\n\nAffirm a stricter posture in `team.md` if the team commits to one."
    }
  ],
  "obligations": {
    "strategy": "minimal",
    "strategy_volume": [
      "One verifiable test per requirement at the narrowest effective level.",
      "At least one happy-path unit test per component.",
      "Unit tests are the default; a bugfix/security scope floor may require an integration or E2E regression when that is the narrowest level that reproduces the defect."
    ],
    "scope_floor": [
      "Keep the existing test suite green.",
      "This scope adds no extra new-test floor beyond the selected test strategy."
    ],
    "combination_rule": "Apply every selected-strategy obligation and every scope-floor obligation; neither replaces the other, and a targeted scope regression may add the narrowest necessary test type beyond the strategy default."
  },
  "plan_profile": {
    "methodology": "test-after",
    "runner_step": "Verify the existing test runner/configuration and record the exact unit-scoped command.",
    "runner_ready_before_first_test": true,
    "testable_layers": [
      "Data model / database behavior",
      "Repository / data access",
      "Business logic",
      "API / endpoint",
      "Frontend behavior"
    ],
    "steps": [
      "Project structure and production configuration skeleton.",
      "Verify the existing test runner/configuration and record the exact unit-scoped command.",
      "Data model / database behavior - implement.",
      "Data model / database behavior - write and run its tests after implementation.",
      "Repository / data access - implement.",
      "Repository / data access - write and run its tests after implementation.",
      "Business logic - implement.",
      "Business logic - write and run its tests after implementation.",
      "API / endpoint - implement.",
      "API / endpoint - write and run its tests after implementation.",
      "Frontend behavior - implement.",
      "Frontend behavior - write and run its tests after implementation.",
      "Environment/build configuration.",
      "Documentation and traceability."
    ]
  },
  "input_sha256": "sha256:c8f84e35190e007d9863fad236154d0269ab9bf633fc2a3ff106ed965ba4a9b9",
  "contract_sha256": "sha256:bb76c1b22f8307be6e5780d50cabab891684449d5b164bd0e825e9e4e5c61e5b"
}
```

