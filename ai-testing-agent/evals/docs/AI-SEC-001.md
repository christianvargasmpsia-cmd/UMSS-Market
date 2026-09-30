# AI-SEC-001 — Invocación indebida del proveedor ante entrada fuera de límite

## 1. Identificación

- **ID:** AI-SEC-001
- **Ataque relacionado:** RT-001
- **Categoría:** Validación de entrada / control de invocación del proveedor
- **Activo:** Proveedor LLM
- **Actor:** Usuario no privilegiado
- **Estado:** MITIGADO
- **Severidad:** Alta

## 2. Descripción

El ataque RT-001 intenta provocar la invocación del proveedor LLM mediante una entrada que supera el límite permitido de tamaño.

La condición observable del ataque es:

```text
proveedor_invocado
```

El escenario se considera exitoso cuando el proveedor externo es invocado a pesar de que la entrada supera el límite permitido.

## 3. Ataque exacto

Archivo:

```text
ataques/RT-001.json
```

El ataque utiliza una entrada de tamaño superior al límite permitido y define como criterio observable:

```text
proveedor_invocado
```

El criterio no depende de una comparación exacta de texto. Se verifica mediante la observación de si el proveedor fue realmente invocado durante la ejecución.

## 4. Precondición

La aplicación debe estar habilitada para procesar solicitudes mediante el servicio de IA.

El ataque se ejecuta sobre el servicio propio de pruebas utilizando fixtures y mocks controlados.

La prueba no utiliza datos reales ni realiza llamadas a servicios de producción.

## 5. Pasos reproducibles

Desde la raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\redteam.ps1
```

El runner ejecuta RT-001 tres veces para obtener una observación repetida del comportamiento.

Los resultados se almacenan en:

```text
reports/redteam/antes.json
reports/redteam/despues.json
```

## 6. Resultado baseline / antes de la mitigación

Archivo de evidencia:

```text
reports/redteam/antes.json
```

En el escenario baseline, con las mitigaciones desactivadas, RT-001 produjo:

```text
3 ejecuciones
3 éxitos
```

El proveedor fue invocado en las tres repeticiones.

Los valores observados fueron:

```text
success_count = 3
total = 3
```

Por tanto:

```text
Attack Success Rate = 3 / 3 = 1.0
```

El resultado demuestra que el escenario era reproducible cuando la mitigación se encontraba desactivada.

## 7. Mitigación

La aplicación incorpora una validación del tamaño máximo de la entrada antes de permitir la invocación del proveedor.

Cuando la entrada excede el límite permitido, el flujo debe detenerse antes de realizar la llamada al proveedor y devolver una respuesta de validación:

```text
AI_INPUT_TOO_LONG
```

La finalidad del control es impedir que una entrada que incumple las restricciones de tamaño llegue al proveedor externo.

## 8. Resultado después de la mitigación

Archivo de evidencia:

```text
reports/redteam/despues.json
```

Después de activar las mitigaciones, RT-001 produjo:

```text
3 ejecuciones
0 éxitos
```

El proveedor no fue invocado en ninguna de las tres repeticiones.

Los valores observados fueron:

```text
success_count = 0
total = 3
```

Por tanto:

```text
Attack Success Rate = 0 / 3 = 0.0
```

La respuesta observada para la entrada fuera de límite corresponde al rechazo de la entrada mediante:

```text
AI_INPUT_TOO_LONG
```

## 9. Impacto

Sin la mitigación, una entrada fuera de los límites podía llegar al proveedor externo.

Los impactos potenciales del escenario controlado incluyen:

- procesamiento innecesario de entradas inválidas;
- consumo indebido del proveedor;
- incremento innecesario de costos;
- ampliación de la superficie de ataque;
- pérdida de control sobre las condiciones de entrada.

El impacto se evalúa sobre el escenario controlado y no implica que estos efectos hayan ocurrido en producción.

## 10. Evidencia

Las evidencias relacionadas con este hallazgo son:

```text
ataques/RT-001.json
reports/redteam/antes.json
reports/redteam/despues.json
evidence/E06-v3-metricas-quality-gate.png
evidence/E10-archivos-verificados.png
```

El archivo `RT-001.json` define el ataque y su criterio observable.

El archivo `antes.json` contiene el resultado del escenario baseline con las mitigaciones desactivadas.

El archivo `despues.json` contiene el resultado del escenario protegido con las mitigaciones activadas.

## 11. Criterio de éxito observable

El ataque utiliza el siguiente criterio:

```text
proveedor_invocado
```

El ataque se considera exitoso cuando el proveedor es invocado durante la ejecución.

No se utiliza comparación exacta de una respuesta textual como criterio de seguridad.

Esto permite comprobar directamente el comportamiento de la integración con el proveedor.

## 12. Verificación

El escenario fue repetido tres veces en cada estado.

| Estado | Ejecuciones | Éxitos | Attack Success Rate |
|---|---:|---:|---:|
| Antes de la mitigación | 3 | 3 | 1.0 |
| Después de la mitigación | 3 | 0 | 0.0 |

La comparación muestra que el comportamiento observable cambió de:

```text
proveedor invocado
```

a:

```text
proveedor no invocado
```

cuando la mitigación fue activada.

## 13. Resultado del control

El control bloquea el escenario RT-001 después de activar la validación de entrada.

La condición que anteriormente permitía que el proveedor fuera invocado dejó de producir un resultado exitoso en las tres repeticiones protegidas.

Resultado final:

```text
Attack Success Rate = 0.0
```

## 14. Conclusión

RT-001 fue reproducible con las mitigaciones desactivadas y dejó de ser exitoso después de activar la validación del tamaño de entrada.

La prueba proporciona evidencia reproducible de que, para el escenario controlado, una entrada que supera el límite permitido no provoca la invocación del proveedor.

El control se considera verificado para este escenario de Red Team.

La evidencia corresponde a una prueba determinista realizada sobre fixtures y mocks del entorno de evaluación y no constituye, por sí sola, una garantía de seguridad del sistema en producción.
