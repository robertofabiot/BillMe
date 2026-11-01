# Imagen del backend para Render (plan gratis: 512 MB de RAM).
# Los tests corren en el CI; aquí solo se compila y empaqueta.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 billme
COPY --from=build /app/target/BillMe-*.jar app.jar
USER billme

ENV SPRING_PROFILES_ACTIVE=prod
# Heap acotado para dejar memoria al resto de la JVM; arranque rápido para el despertar en frío
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
