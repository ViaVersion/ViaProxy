FROM eclipse-temurin:25-jre-alpine
WORKDIR /app/run
COPY /build/libs/ViaProxy-*.jar /app/ViaProxy.jar
ENTRYPOINT ["java", "-jar", "/app/ViaProxy.jar", "config", "viaproxy.yml"]
