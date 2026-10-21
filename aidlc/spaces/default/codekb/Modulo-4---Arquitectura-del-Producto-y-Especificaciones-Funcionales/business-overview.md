# Contexto del negocio

UMSS Market es el marketplace académico descrito por el proyecto. Este análisis parcial cubre exclusivamente la comprobación operativa del backend y su base de datos; las funciones comerciales se identificaron por nombres de paquetes, sin evaluar sus contratos.

## Funcionalidad relevante

GET /api/health ya existe y permite comprobar que el proceso HTTP responde y que una consulta SQL real obtiene el resultado esperado. No escribe datos ni participa en pagos, pedidos o reservas. Contrato completo en api-documentation.md.
