 
# AI-SEC-003 — Acceso al historial sin autenticación

## 1. Identificación

- ID: AI-SEC-003
- Ataque relacionado: RT-003
- Categoría: Autenticación / protección de historial
- Activo: Historial de conversaciones del usuario
- Actor: Usuario anónimo
- Estado: CONTROL PREVENTIVO VERIFICADO
- Severidad: Alta

## 2. Descripción

RT-003 intenta acceder al historial de conversaciones sin contar con una identidad autenticada.

La condición observable del ataque es:

```text
secret_exposed