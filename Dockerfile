FROM node:24-alpine AS frontend-build
WORKDIR /app/admin-web

COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci

COPY admin-web/ ./
RUN npm run build

FROM maven:3.9.8-eclipse-temurin-21 AS backend-build
WORKDIR /app

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
ENV GITHUB_USERNAME=${GITHUB_USERNAME}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

RUN mkdir -p /root/.m2
RUN printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' \
    '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
    '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' \
    '    <servers>' \
    '        <server>' \
    '            <id>github</id>' \
    '            <username>${env.GITHUB_USERNAME}</username>' \
    '            <password>${env.GITHUB_TOKEN}</password>' \
    '        </server>' \
    '    </servers>' \
    '</settings>' > /root/.m2/settings.xml

COPY pom.xml ./
RUN mvn -q -s /root/.m2/settings.xml -DskipTests dependency:go-offline

COPY src ./src
COPY --from=frontend-build /app/admin-web/dist ./src/main/resources/static
RUN mvn -q -s /root/.m2/settings.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-build /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
