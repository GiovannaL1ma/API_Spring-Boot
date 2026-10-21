# ---------- Etapa de build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# copia o pom primeiro para aproveitar o cache das dependencias
COPY pom.xml .

# baixa as dependencias (acelera os proximos builds)
RUN mvn -q -DskipTests dependency:go-offline

# copia o codigo e gera o .jar
COPY src src
RUN mvn -DskipTests clean package


# ---------- Etapa de execucao ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# o Render define a variavel PORT quando sobe o container
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
