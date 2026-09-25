# Red teaming de UMSS Market

## Ejecutar
Desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File ai-testing-agent/scripts/redteam.ps1
```

Requiere Java 21 y Maven (wrapper del backend por defecto). Parámetros opcionales: -MavenCommand ruta/a/mvn.cmd y -MavenRepository ruta/al/repositorio/local.

La suite usa el servicio Spring Boot real con proveedor y casos de uso simulados: no requiere credenciales, BD ni Ollama. El ejecutor devuelve error si un ataque tiene éxito o falla Maven; guarda el reporte en reports/redteam/despues.json. Cinco casos por tres repeticiones.

## Formato
Cada archivo de ataques/ es un array, con los campos del AI Security Lab: id, titulo, categoria, owasp, atlas, mensaje, documento, usar_conocimiento, exito_si y comportamiento_esperado.

Extensiones del adaptador Java: fixture (decisión adversaria del proveedor e identidad de sesión), repeticiones y criterios proveedor_invocado, herramienta_ejecutada y usuario_consultado. Estos criterios requieren este ejecutor; no se afirma compatibilidad directa con demo.py del laboratorio.

Las copias en backend/src/test/resources/redteam/ataques permiten ejecutar Maven de forma independiente. El script pasa ataques/ como fuente explícita. Mantener ambas copias sincronizadas al editar los casos.

## Evidencia
Consultar [evidencia/redteam](../evidencia/redteam/RESUMEN.md) y [hallazgo AI-SEC-001](../docs/hallazgos/AI-SEC-001.md).
- antes.json: guarda retirada en una copia aislada.
- despues.json: protección activa.
- sin-mitigacion.json y mutacion.json: prueba negativa que confirma que la regresión detecta retirar la guarda.
- jacoco.csv: cobertura. ChatInputPolicy tiene 100% de líneas y ramas; no se afirma cobertura global del backend.

## Demostración con modelo real: avance documentado
El usuario aportó doce respuestas de Postman con Ollama: seis intentos de inyección indirecta en dos variantes y seis consultas a una publicación limpia. Ningún intento incluyó la marca objetivo. Consultar [informe RAG](../evidencia/redteam/ollama/INFORME_RAG.md) y [respuestas transcritas](../evidencia/redteam/ollama/experimento-rag.json).

Estas respuestas no fueron capturadas por redteam.ps1, que sigue usando mocks. Las negativas observadas no prueban éxito del ataque, ni la comparación con otra publicación demuestra causalidad.

Falta ejecutar los cinco casos principales contra el modelo con trazas por corrida y el antes/después real de RT-001 para completar esa demostración ampliada. Los cinco entregables controlados ya existen; ver el [estado de la consigna](../docs/ESTADO_CONSIGNA_REDTEAM.md) para sus límites.
