# Calidad y límites de la evidencia

## Pruebas

Se leyeron seis métodos @Test en tres clases. Cubren respuesta saludable, 503 sin detalles internos, fallo de conexión, timeout, resultado SQL inesperado y petición anónima integrada. Los mocks verifican cierre de conexión/statement y, cuando corresponde, ResultSet.

El antecedente comunicado por el coordinador es 6 pruebas exitosas, 14/14 líneas y 4/4 ramas del feature. Es evidencia histórica de la conversación, no una ejecución ni medición realizada por este escaneo. Debe obtenerse evidencia vigente en Build and Test.

## Cobertura y herramientas

pom.xml configura JaCoCo verify con 90% de líneas y 80% de ramas a nivel BUNDLE, con exclusiones de persistencia/adaptadores. No hay perfil específico de salud entre los perfiles leídos. Mantener los mínimos por feature de AGENTS.md; un reporte agregado no demuestra por sí solo esa cobertura. No se inspeccionaron CI/CD, linters ni documentación externa al alcance.

## Evaluación acotada

Nombres y separación de responsabilidades: buenos. Tamaño de métodos: bueno. Manejo de errores: adecuado al contrato HTTP, aunque la sonda no registra diagnóstico. Testabilidad: buena. No se identificó duplicación relevante en las dos clases de salud.

## Riesgos y deuda fuera del alcance

El timeout de statement no acota adquisición de conexión ni arranque de la aplicación con BD caída. H2 no demuestra comportamiento de PostgreSQL real. SecurityConfig usa BCryptPasswordEncoder() sin costo explícito, discrepancia existente con la regla de costo mínimo 12; se registra sin modificarla ni incorporarla al feature.
