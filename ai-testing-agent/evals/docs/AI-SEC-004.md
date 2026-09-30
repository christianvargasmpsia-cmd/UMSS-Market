 
# AI-SEC-004 — Bypass de política de acceso a recomendaciones

## 1. Identificación

- ID: AI-SEC-004
- Ataque relacionado: RT-004
- Categoría: Bypass de política / autorización
- Activo: Servicio de recomendaciones
- Actor: Usuario no autenticado
- Estado: CONTROL PREVENTIVO VERIFICADO
- Severidad: Alta

## 2. Descripción

RT-004 intenta provocar la ejecución de recomendaciones para un actor que no cumple las condiciones de autorización.

La condición observable definida para el ataque es:

```text
policy_bypass