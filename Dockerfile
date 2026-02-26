# ---- Stage 1: Build Vue.js frontend ----
FROM node:20-alpine AS frontend-build

WORKDIR /app

# Copy root package.json (needed for the @org/source file:../.. dependency)
COPY package.json ./

# Copy web app package files and install dependencies
COPY apps/web/package.json apps/web/package-lock.json* apps/web/
RUN cd apps/web && npm install

# Copy web app source
COPY apps/web/ apps/web/

# Build frontend with empty API URL (same-origin in production)
RUN cd apps/web && VUE_APP_API_URL= VUE_APP_APPLE_AFFILIATE_TOKEN= npx vue-cli-service build


# ---- Stage 2: Build Java backend ----
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY apps/backend/pom.xml .
RUN mvn dependency:resolve -B

# Copy backend source
COPY apps/backend/src src

# Copy the built frontend into Spring Boot's static resources
COPY --from=frontend-build /app/apps/web/dist/ src/main/resources/static/

# Build the Spring Boot fat JAR (skip tests)
RUN mvn package -DskipTests -B


# ---- Stage 3: Runtime ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=backend-build /app/target/*.jar app.jar

# Railway provides PORT env var
ENV PORT=8080
EXPOSE ${PORT}

# Run the application
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
