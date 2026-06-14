ARG DOCKER_REGISTRY=
FROM ${DOCKER_REGISTRY}maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml

COPY pom.xml ./
RUN mvn dependency:resolve -B

COPY src ./src
RUN mvn clean package -DskipTests -B && \
    rm -rf /root/.m2/repository

FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre

WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8088
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
