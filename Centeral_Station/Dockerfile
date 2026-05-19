FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

VOLUME /app/data

COPY --from=build /app/target/*.jar central-station.jar

RUN sed -i 's/10 ms/0 ms/g' $JAVA_HOME/lib/jfr/profile.jfc && \
    sed -i 's/20 ms/0 ms/g' $JAVA_HOME/lib/jfr/profile.jfc
    
EXPOSE 8080

# ENTRYPOINT ["java", "-jar", "central-station.jar"]

# --- PROFILING NOTE ---
# To profile the central station using JFR for 1 minute for your report,
# uncomment the line below and comment out the ENTRYPOINT above:
ENTRYPOINT ["java", "-XX:StartFlightRecording=duration=60s,settings=profile,filename=/app/data/central_profile.jfr", "-jar", "central-station.jar"]