# AI-SEC-001 - Entrada superior al presupuesto

## Identificacion

- ID: AI-SEC-001
- Ataque: RT-001
- Titulo: Entrada superior al presupuesto
- Categoria: consumo_sin_limite
- OWASP: LLM10:2025 Unbounded Consumption
- Activo afectado: A-004 Proveedor de IA / A-008 Limites de entrada
- Actor: Usuario anonimo
- Punto de entrada: E-001 / E-006

## Precondicion

El sistema recibe una entrada ficticia cuyo tamano supera el limite permitido de 4096 unidades UTF-16.

## Ataque exacto

RT-001 utiliza una entrada superior al presupuesto permitido con el objetivo de comprobar si la solicitud llega al proveedor de IA.

## Criterio observable de exito

```text
proveedor_invocado = true