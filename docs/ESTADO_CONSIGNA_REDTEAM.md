# Estado de la consigna de red teaming

Revisión documental: 2026-09-24. No se volvieron a ejecutar tests ni ataques durante esta actualización de informes.

| Requisito | Evidencia disponible | Estado y límite |
|---|---|---|
| Modelo de amenazas en siete filas | MODELO_DE_AMENAZAS.md | Preparado con la estructura del laboratorio y contexto del producto. |
| Cinco ataques JSON, criterio observable, tres corridas | ai-testing-agent/ataques/RT-001 a RT-005; evidencia/redteam/antes.json y despues.json | Preparado y ejecutado sobre servicio real con proveedor/dependencias simulados. Incluye extensiones de criterio y fixture del adaptador Java; no es ejecución directa con demo.py del laboratorio. |
| Hallazgo completo AI-SEC-xxx | hallazgos/AI-SEC-001.md | Documentado: entrada excesiva alcanza el proveedor; 3/3 antes. No demuestra costo, caída ni explotación con Ollama real. |
| Mitigación en código y retest del mismo ataque | ChatInputPolicy y AIServiceImpl.chat; despues.json | Implementado y verificado en la suite controlada: 0/3 después. |
| Regresión que falle al quitar la mitigación, sin igualdad de texto | RedTeamSecurityTest, ChatInputPolicyTest, mutacion.json y sin-mitigacion.json | Verificado en copia aislada: retirar la guarda hace fallar RT-001; restaurarla deja pasar los tests. |

## ¿Está completo al 100 %?

Los cinco entregables existen y cuentan con evidencia para el alcance de pruebas controladas con dobles. El enunciado compartido no exige explícitamente que todas las pruebas usen un modelo real, por lo que no se puede convertir esa condición en un requisito textual nuevo. Tampoco se puede garantizar la aceptación o calificación del docente: debe quedar claro que los cinco casos principales no se ejecutaron contra Ollama.

La demostración ampliada con modelo real aún no está completa. Hay dos variantes de una misma inyección indirecta, tres corridas por variante, y seis controles limpios reportados por el usuario. No equivalen a cinco ataques distintos ni a un antes/después de la mitigación de AI-SEC-001. No se observó la marca objetivo y no se documenta un nuevo hallazgo explotado.

## Para cerrar la demostración ampliada con Ollama

1. Ejecutar los cinco casos a través del backend conectado al modelo, con sesiones y datos ficticios; ignorar las decisiones impuestas por fixture en el modo real.
2. Guardar trazas de proveedor/herramientas por corrida, solicitud, respuesta, modelo efectivo, configuración y errores. Un fallo de conexión no cuenta como bloqueo.
3. Repetir RT-001 antes/después en una instancia aislada y comprobar la llamada real al proveedor antes y su ausencia después. Una consulta legítima debe seguir funcionando.
4. Mantener los tests deterministas y su evidencia de retirada de la guarda; complementar, sin reemplazar, los resultados con modelo real.

La inyección indirecta puede conservarse como caso adicional con resultado negativo. No hace falta forzar su éxito ni cambiar su criterio para cumplir la consigna.

## Archivos de consulta

- [Resumen de evidencia](../evidencia/redteam/RESUMEN.md).
- [Experimento manual con Ollama](../evidencia/redteam/ollama/INFORME_RAG.md).
- [Hallazgo AI-SEC-001](hallazgos/AI-SEC-001.md).
