# AI-SEC-001 — Invocación indebida del proveedor ante entrada fuera de límite

## 1. Identificación

- ID: AI-SEC-001
- Ataque relacionado: RT-001
- Categoría: Validación de entrada / control de invocación del proveedor
- Activo: Proveedor LLM
- Actor: Usuario no privilegiado
- Estado: MITIGADO
- Severidad: Alta

## 2. Descripción

El ataque RT-001 intenta provocar la invocación del proveedor LLM mediante una entrada que supera el límite permitido de tamaño.

La condición observable del ataque es:

```text
proveedor_invocado