# Feature 3 - Deteccion de publicaciones potencialmente duplicadas

Responsable: Christian Bernardo Vargas Sandoval. Fecha: 19 de octubre de 2026.

## Contrato HTTP

`POST /api/stores/{storeId}/publications/duplicates`

Requiere Bearer JWT con rol EMPRENDEDOR. El usuario debe existir y conservar ese
rol en persistencia. La tienda debe pertenecer al UUID autenticado; no se acepta
una identidad del cuerpo de la solicitud. Esta operacion solo consulta datos.

Body para crear una publicacion:

```json
{
  "title": "Laptop Lenovo ThinkPad",
  "description": "Computadora portatil con 16 GB de RAM y disco SSD de 512 GB"
}
```

Para evaluar una actualizacion, agregar `excludePublicationId` con el UUID de la
publicacion actual. Se valida que exista y pertenezca a la tienda; se excluye antes
de generar embeddings. No se crea ni actualiza una publicacion con este endpoint.

title: obligatorio, no vacio, hasta 200 caracteres. description: obligatoria, no
vacia, hasta 2000 caracteres. excludePublicationId: UUID opcional o null.

```bash
curl -X POST "$BASE_URL/api/stores/$STORE_ID/publications/duplicates" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Laptop Lenovo ThinkPad","description":"Computadora portatil con 16 GB de RAM y disco SSD de 512 GB"}'
```

Respuesta ilustrativa HTTP 200 (las puntuaciones reales dependen del modelo):

```json
{
  "storeId": "11111111-1111-1111-1111-111111111111",
  "status": "EVALUATED",
  "message": "Posibles coincidencias para revision humana. La deteccion no bloquea ni modifica publicaciones.",
  "threshold": 0.9,
  "maxResults": 5,
  "matches": [
    {
      "publicationId": "22222222-2222-2222-2222-222222222222",
      "title": "Laptop Lenovo ThinkPad",
      "similarity": 1.0
    }
  ]
}
```

| Caso | Resultado |
|---|---|
| Tienda vacia o solo la publicacion excluida | 200, matches vacio, sin llamar a Ollama |
| Comparacion correcta sin coincidencias sobre el umbral | 200, matches vacio |
| Sin autenticacion o rol JWT incorrecto | 403, configuracion actual de Spring Security |
| JWT invalido o usuario inexistente | 401 |
| Tienda o publicacion excluida ajena; rol persistido incorrecto | 403 |
| Tienda o publicacion excluida inexistente | 404 |
| Campos invalidos, JSON malformado o UUID incorrecto | 400 |
| Embeddings ocupados, timeout, HTTP incorrecto o vector invalido | 503, mensaje con EMBEDDINGS_UNAVAILABLE; no devuelve matches vacio |

## Algoritmo y aislamiento

1. Verificar identidad, rol, propiedad de la tienda y exclusion solicitada.
2. Consultar en SQL solamente publicaciones de esa tienda, incluidas inactivas y
   sin stock: tambien pueden ser duplicados. No compara imagenes ni otras tiendas.
3. Construir `Titulo: ...\nDescripcion: ...` para solicitud y candidatos. Quita
   espacios en extremos; no incluye precio, stock ni categoria. Deduplica textos
   identicos dentro de la misma solicitud.
4. Reutilizar Ollama y el modelo nomic-embed-text existente mediante /api/embed.
   Un puerto dedicado permite solicitud por lote, CPU, timeout total, validacion
   estricta y fallo explicito. No usa qwen2.5:1.5b para generar vectores.
5. Validar cantidad, dimensiones, valores finitos y norma no nula de TODOS los
   vectores. Un candidato invalido invalida la evaluacion completa; nunca se omite
   silenciosamente para aparentar ausencia de duplicados.
6. Calcular similitud coseno con normalizacion numericamente estable. Textos
   identicos validos tienen puntuacion 1. Filtrar score >= threshold, ordenar de
   mayor a menor (UUID como desempate) y limitar a maxResults.

No se reutilizan los embeddings persistidos anteriores: incluyen otros campos y
no tienen version de modelo/texto; se regeneran los de titulo/descripcion juntos
para evitar mezclar espacios vectoriales o datos obsoletos. No hay migracion SQL.

## Configuracion externa

| Variable | Por defecto | Restriccion |
|---|---|---|
| DUPLICATES_THRESHOLD | 0.90 | Numero finito entre 0 y 1, inclusivo |
| DUPLICATES_MAX_RESULTS | 5 | Entero entre 1 y 50 |
| DUPLICATES_OLLAMA_BASE_URL | http://localhost:11434 | URL del servidor Ollama |
| DUPLICATES_OLLAMA_MODEL | nomic-embed-text | Modelo de embeddings instalado |
| DUPLICATES_OLLAMA_TIMEOUT_MS | 45000 | Positivo; incluye recepcion de respuesta |

Reiniciar backend tras cambiar variables. Configuracion invalida de umbral/limite
impide el inicio. El adaptador fuerza num_gpu=0 para este equipo y conserva el
modelo 5 minutos. truncate=false impide ignorar silenciosamente parte del texto.
Una solicitud de embeddings por instancia, sin cola ni reintentos automaticos.

Si la carga en frio del disco supera el timeout, precargar el modelo antes de la
demostracion. Esto no modifica el timeout del endpoint:

```powershell
$body = @{
  model = 'nomic-embed-text'
  input = ''
  keep_alive = '10m'
  options = @{ num_gpu = 0 }
} | ConvertTo-Json -Depth 4
Invoke-RestMethod 'http://localhost:11434/api/embed' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 180
```

## Limites

Las coincidencias son advertencias: similitud no demuestra duplicidad real. Puede
haber falsos positivos/negativos, especialmente entre productos de una familia.
El umbral debe calibrarse con ejemplos reales del catalogo y del idioma. Se comparan
todas las publicaciones de la tienda en un lote; costo y memoria crecen con el
catalogo. Una tienda grande o carga en frio puede superar el timeout y devolver
503. No hay indice vectorial, cache persistente ni procesamiento incremental.
La consulta es una observacion del catalogo en ese momento; no reserva nombres ni
garantiza que otra publicacion no sea creada simultaneamente. No bloquea CRUD,
elimina, fusiona ni cambia publicaciones. No agrega interfaz grafica.

## Validacion

Desde backend/umss-market-api, con Java 21 y Maven:

```powershell
./mvnw.cmd -Pduplicates-coverage '-Dtest=DuplicateDetectionUnitTest,DuplicateDetectionIntegrationTest,DuplicateEmbeddingAdapterTest' '-Djacoco.append=false' verify
./mvnw.cmd '-Dtest=DuplicateEmbeddingLiveTest' test
```

La segunda prueba requiere Ollama y nomic-embed-text; la primera usa H2 y un servidor
HTTP controlado. El perfil aplica JaCoCo >=90% lineas y >=80% ramas al feature y a
SecurityConfig. Reportes en target/site/jacoco/ y target/surefire-reports/.

## Evidencia local del 19 de octubre de 2026

- 14 pruebas del feature aprobadas: 6 unitarias, 4 de integracion JWT/HTTP/JPA/H2 y
  4 del transporte HTTP de embeddings (errores, deadline, concurrencia y recuperacion).
- JaCoCo: 131/131 lineas y 96/96 ramas (100% en ambas); umbrales aprobados.
- Prueba adicional real con nomic-embed-text aprobada: 1277 ms precargado, identica
  1.0, similar 0.9440696649582079; pan integral excluido frente a una laptop con
  threshold=0.90. No constituye calibracion general de precision del modelo.
- La primera llamada agoto 60 segundos cargando el modelo. Precarga posterior:
  143321 ms. Durante carga en frio el endpoint mantiene su timeout y devuelve 503.
- Suite general: 211 pruebas, 210 aprobadas y 1 fallo en
  TransactionalOutboxTest.retriesAfterTemporaryFailureAndRecovers (esperaba RETRY,
  obtuvo PENDING), fuera del feature. Ese fallo ya se observo antes de este cambio;
  no se declara aprobada la suite global.

La validacion no modifico datos de produccion ni publico commits. AGENTS.md y
docs/DTI.md describen la misma decision arquitectonica y deben incluirse juntos
cuando se cree el commit.
