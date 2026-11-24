FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN --mount=type=cache,id=achados-tp3-maven,target=/root/.m2,sharing=locked sed -i "s/\r$//" mvnw && sh ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /data

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
