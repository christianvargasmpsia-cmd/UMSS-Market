# M8-DEP-002 - Persistencia de resenas requerida por Feature 2

Estado inicial: ABIERTA (registrada antes de implementar).
Estado actual: RESUELTA en el codigo local (2026-10-19).
Fecha: 2026-10-19. Responsable: Christian Bernardo Vargas Sandoval.
Tipo: dependencia funcional / issue local versionado; no publicado en GitHub.

La inspeccion del backend no encontro entidad, repositorio, migracion ni endpoints
de resenas. InteractionType.COMMENT representa interacciones, no una resena persistida
con contrato de validacion y consulta por producto.

Bloquea: [Feature 2 - persistencia y resumen](../M8_FEATURE_2_REVIEWS.md).

Resolucion requerida: tabla product_reviews asociada a users y publications,
migracion SQL, creacion autenticada, consulta por publicacion y pruebas de persistencia.
El resumen solo se habilita sobre esa persistencia; no usa comentarios simulados.

No se requieren edicion o eliminacion de resenas ni verificacion de compra previa.

## Evidencia de resolucion

ProductReview, JpaReviewRepositoryAdapter y ProductReviewsController permiten guardar
y recuperar resenas asociadas al comprador y a la publicacion. ProductReviewsIntegrationTest
verifica la persistencia tras limpiar el contexto JPA; ReviewMigrationTest aplica el SQL
y verifica sus restricciones. Suite local: 196 pruebas aprobadas.

La dependencia de persistencia queda resuelta; el resumen consume exclusivamente
ReviewRepositoryPort. Ver [documentacion del feature](../M8_FEATURE_2_REVIEWS.md).
