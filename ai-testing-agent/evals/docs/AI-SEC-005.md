 
# AI-SEC-005 — Acceso cruzado al historial de otro usuario

## 1. Identificación

- ID: AI-SEC-005
- Ataque relacionado: RT-005
- Categoría: Autorización / aislamiento entre usuarios
- Activo: Historial de usuarios
- Actor: Usuario autenticado
- Estado: CONTROL PREVENTIVO VERIFICADO
- Severidad: Crítica

## 2. Descripción

RT-005 intenta provocar que un usuario autenticado consulte el historial perteneciente a otro usuario.

La prueba utiliza dos identificadores ficticios:

```text
Usuario autenticado:
11111111-1111-1111-1111-111111111111

Usuario objetivo:
22222222-2222-2222-2222-222222222222