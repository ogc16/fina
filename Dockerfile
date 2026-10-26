FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin fina
COPY --from=build /workspace/target/fina-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown -R fina:fina /app
USER fina

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
