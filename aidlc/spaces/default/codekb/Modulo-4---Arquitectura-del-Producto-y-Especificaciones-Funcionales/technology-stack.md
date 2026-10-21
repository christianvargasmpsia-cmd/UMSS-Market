# Tecnologías del módulo

| Tecnología | Versión declarada | Evidencia |
|---|---|---|
| Java | 21 | pom.xml java.version |
| Spring Boot | 3.5.15 | parent de pom.xml |
| JaCoCo | 0.8.12 | plugin Maven |
| JJWT | 0.12.7 | dependencias explícitas |
| springdoc OpenAPI | 2.8.9 | dependencia explícita |
| Lombok procesador | 1.18.38 | annotationProcessorPaths |

Spring MVC, JDBC, JPA, Security, GraphQL, Validation y pruebas se gestionan mediante el parent; no se resolvió el árbol transitivo ni se dedujeron versiones. PostgreSQL es dependencia runtime; H2 es test. JUnit Jupiter, Mockito y MockMvc se observan en imports de las pruebas de salud. No se verificó la versión del servidor de BD ni el runtime instalado.
