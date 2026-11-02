# E3 - Diseño de extracción del módulo de reseñas

## Producto
UMSS Market - Spring Boot, Java 21 y PostgreSQL.

## Pregunta de investigación
¿Conviene extraer el módulo de reseñas de productos a
un servicio independiente en lugar de mantenerlo
dentro del monolito modular?

## Motivo de extracción
Evaluar si el aislamiento de fallas justifica
el costo adicional de comunicación y despliegue.

## Frontera funcional
El módulo de reseñas será responsable de:
- Guardar reseñas.
- Consultar reseñas.
- Procesar resúmenes de reseñas.

## Dependencias identificadas
- PublicationRepositoryPort
- UserRepositoryPort
- ReviewRepositoryPort
- ReviewSummaryPort

## Distribución de responsabilidades

API principal:
- Autenticación.
- Autorización.
- Validación de usuarios.
- Validación de existencia de publicaciones.
- Exposición del contrato HTTP público.

Módulo de reseñas:
- Persistencia de reseñas.
- Consulta de reseñas.
- Procesamiento de resúmenes.

## Contrato arquitectónico
Se implementará ReviewsPort con dos adaptadores:

1. ReviewsLocalAdapter
2. ReviewsHttpAdapter

La implementación se seleccionará mediante
una variable de entorno.

## Formas de ejecución

A - Monolito modular:
API y módulo de reseñas en el mismo proceso.

B - Servicio extraído:
API -> HTTP -> Toxiproxy -> servicio de reseñas.

Ambas formas utilizarán la misma imagen Docker
con distintos puntos de entrada.

## Datos
Tabla: product_reviews.

La tabla mantiene referencias a:
- publications
- users

Se conservará inicialmente la instancia PostgreSQL,
pero el acceso a product_reviews se encapsulará
dentro del módulo de reseñas.

La base compartida es una limitación arquitectónica
que deberá declararse en el informe y ADR.

## Rutas de medición

Ruta dependiente:
GET /api/publications/{publicationId}/reviews?limit=20

Ruta independiente:
GET /api/publications/{id}

## Control de comparabilidad
- Mismos datos y UUID existentes.
- Misma imagen y código de negocio.
- Misma máquina.
- Mismo ritmo de solicitudes k6.
- Mismas respuestas funcionales.
- Calentamiento descartado.
- Cinco corridas como mínimo por forma.
- Reinicio limpio entre corridas.

## Riesgos
- Dependencia de una base de datos compartida.
- Sobrecosto HTTP.
- Mayor consumo de recursos por dos procesos.
- Dependencia externa de Ollama en los resúmenes.
- Fallas artificiales diferentes a incidentes reales.

El benchmark principal utilizará consultas de
reseñas, sin ejecutar inferencia de IA.

## Estado
Diseño arquitectónico propuesto.
Sin mediciones realizadas.
Pendiente de implementación y validación.
