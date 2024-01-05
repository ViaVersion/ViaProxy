FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk as builder
WORKDIR /build
RUN apt-get update && apt-get install -y \
    git
COPY . .
RUN ./gradlew build

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar ViaProxy.jar
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]