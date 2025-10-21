# S1+S2 Unified Monitoring (Java 21, Spring Boot 3.3)

## Build & Run
```bash
mvn -q -DskipTests package
java -jar target/s1s2-monitoring-1.0.0.jar
```
OpenAPI UI: http://localhost:8080/swagger-ui/index.html

## Kafka topics
Configured via `app.topics.*` in `application.yml`.
Bootstrap servers: `spring.kafka.bootstrap-servers` (set to 109.73.202.251:31493 by default).
