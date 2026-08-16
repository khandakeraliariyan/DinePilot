FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY restaurant-service/pom.xml restaurant-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY reservation-service/pom.xml reservation-service/pom.xml
COPY billing-service/pom.xml billing-service/pom.xml
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY common/src common/src
COPY order-service/src order-service/src
RUN ./mvnw -q -pl common,order-service -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
