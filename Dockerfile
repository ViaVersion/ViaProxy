FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk as builder
WORKDIR /build
RUN apt-get update && apt-get install -y \
    git
COPY . .
RUN ./gradlew build

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar .
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]