# Use a full JDK base image on Ubuntu Jammy to ensure JFR and profiling tools are available
FROM eclipse-temurin:21-jdk-jammy

# Create a dedicated directory for the application
WORKDIR /app

# Match the 'data/' base directory defined in application.properties
VOLUME /app/data

# Copy the compiled Spring Boot executable JAR
# (Update the path to build/libs/*.jar if you are using Gradle instead of Maven)
COPY target/*.jar central-station.jar

# Expose the server port exactly as defined in application.properties
EXPOSE 8080

# The standard entry point to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "central-station.jar"]

# --- PROFILING NOTE ---
# To profile the central station using JFR for 1 minute for your report,
# uncomment the line below and comment out the ENTRYPOINT above:
# ENTRYPOINT ["java", "-XX:StartFlightRecording=duration=60s,filename=/app/data/central_profile.jfr", "-jar", "central-station.jar"]