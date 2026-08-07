FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/pom.xml
COPY restaurant-service/pom.xml restaurant-service/pom.xml
RUN chmod +x mvnw

COPY common/src common/src
COPY restaurant-service/src restaurant-service/src
RUN ./mvnw -q -pl common,restaurant-service -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/restaurant-service/target/restaurant-service.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
