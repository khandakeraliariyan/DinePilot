FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY restaurant-service/pom.xml restaurant-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY common/src common/src
COPY api-gateway/src api-gateway/src
RUN ./mvnw -q -pl common,api-gateway -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
