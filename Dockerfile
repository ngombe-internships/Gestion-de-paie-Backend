# Étape 1: Build avec Maven
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copie les fichiers de configuration Maven
COPY pom.xml .
COPY src ./src

# Compile et package l'application (sans les tests pour aller plus vite)
RUN mvn clean package -DskipTests

# Étape 2: Image finale légère avec seulement le JAR
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copie le JAR compilé depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Expose le port 8081
EXPOSE $PORT

# Variables d'environnement pour optimiser la JVM
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Commande pour lancer l'application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]