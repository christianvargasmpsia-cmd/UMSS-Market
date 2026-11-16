# Demo 4 - UC-001: imágenes de publicaciones

## Alcance
Se incorpora carga, listado, consulta y eliminación de imágenes sin alterar el CRUD de publicaciones.

## Almacenamiento gratuito
Los archivos se guardan en un volumen Docker local `publication-images` y los metadatos en PostgreSQL mediante JPA (`publication_images`). No requiere servicios externos. El volumen debe respaldarse y no debe eliminarse con `docker compose down -v`.

## API
- `POST /api/publications/{publicationId}/images` con JWT de emprendedor y `multipart/form-data`, campo `file`.
- `GET /api/publications/{publicationId}/images` lista metadatos y URL de cada imagen.
- `GET /api/publications/{publicationId}/images/{imageId}/content` entrega la imagen.
- `DELETE /api/publications/{publicationId}/images/{imageId}` con JWT del propietario.

Tipos admitidos: JPEG, PNG y WebP; máximo 5 MiB. Se verifica la firma binaria y el tipo declarado. Se genera nombre aleatorio para evitar traversal y colisiones.

## Permisos
El JWT expone UUID como principal y autoridad `ROLE_EMPRENDEDOR`. Se comprueba `StoreRepositoryPort.findByUserId(userId)` y se compara `store.id` con `publication.storeId`. Sin token: 401; rol incorrecto o tienda ajena: 403; recurso inexistente: 404; archivo inválido: 400.

## Ejecución
Desde la raíz del repositorio, añadir `-f compose.images.yaml` a los archivos compose existentes que levanten `api`. Ejemplo: `docker compose -f compose.yaml -f compose.images.yaml up -d --build` (ajustar el nombre del archivo base según el repositorio).

## Pruebas manuales
1. Crear o identificar una publicación perteneciente al emprendedor.
2. En Postman: POST con Bearer JWT, Body form-data, clave `file` de tipo File, seleccionar JPG/PNG/WebP. Verificar 201.
3. Consultar GET de lista y luego GET del `url` retornado. Verificar imagen correcta.
4. Repetir POST sin token (401), con comprador (403), con emprendedor ajeno (403), y con texto renombrado `.jpg` (400).
5. Eliminar con JWT del propietario (204), comprobar que ya no aparece en la lista.

## Limitaciones
El almacenamiento local no se replica entre servidores. No existe procesamiento de miniaturas ni antivirus. La eliminación de metadatos y archivo no es atómica. Las pruebas de integración y el comportamiento de borrado en cascada deben verificarse antes de producción.
