# Modelo de amenazas del asistente UMSS Market

Adaptado de AI Security Lab. Alcance: chat de Spring Boot, router y consultas. Solo usuarios, mensajes y dependencias ficticios en las pruebas.

| Elemento | Pregunta | En UMSS Market |
|---|---|---|
| **Activos** | ¿Qué queremos proteger? | Historial y recomendaciones por usuario, integridad del catálogo, permisos de herramientas, disponibilidad del proveedor IA. |
| **Actores** | ¿Quién podría atacar? | Usuario anónimo, usuario autenticado que suplanta a otro, vendedor que introduce instrucciones en una descripción. |
| **Puntos de entrada** | ¿Por dónde entra información? | POST /api/ai/chat, mensajes, descripciones de publicaciones y tiendas, decisiones de herramientas del modelo. |
| **Fronteras de confianza** | ¿Dónde cambia la confianza? | Mensaje → servicio; catálogo → contexto; modelo → router; sesión autenticada → identidad utilizada para consultar historial. Los mensajes y decisiones del modelo no otorgan permisos. |
| **Ataques** | ¿Qué podría intentar? | Mensaje excesivo (RT-001), herramienta inventada (RT-002), historial anónimo (RT-003), recomendaciones anónimas (RT-004), suplantación de usuario (RT-005). Inyección indirecta en catálogo queda como riesgo pendiente. |
| **Impacto** | ¿Qué pasa si lo consigue? | Consumo innecesario de inferencia, exposición de datos ajenos o consultas no autorizadas. Las pruebas no demuestran una caída ni pérdida económica. |
| **Mitigaciones** | ¿Qué control reduce el riesgo? | Nuevo límite de entrada cruda de 4096 unidades UTF-16 antes de cualquier llamada; router cerrado existente; identidad tomada de SecurityContext; pruebas de regresión. Pendientes: cuotas, límites de salida/contexto y pruebas con modelo real. |

Flujo: usuario → AIController → AIServiceImpl → ChatInputPolicy → AIProviderPort → router → casos de uso → datos.

El límite se comprueba antes de trim(), incluso con IA deshabilitada. No limita el tamaño HTTP antes de deserializar, la concurrencia, el endpoint product-description ni los contextos RAG.
