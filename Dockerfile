# Étape 1: Build avec Maven
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copie les fichiers de configuration Maven
COPY pom.xml .
COPY src ./src

# Compile et package l'application (sans les tests pour aller plus vite)
RUN mvn clean package -DskipTests

# Étape 2: Image finale légère avec seulement le JAR
# CORRECTION ICI : Utilisation de eclipse-temurin au lieu de openjdk obsolète
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copie le JAR compilé depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Expose le port 8081 (ou celui défini par Render)
EXPOSE $PORT

# Variables d'environnement pour optimiser la JVM
ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Commande pour lancer l'application avec le profil de production par défaut
# Ou en utilisant la variable d'environnement $PORT de Render
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar --server.port=${PORT:-8080}"]