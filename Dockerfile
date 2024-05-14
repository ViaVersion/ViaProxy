FROM --platform=$BUILDPLATFORM gradle:jdk17 as builder
WORKDIR /build
COPY build.gradle settings.gradle gradle.properties ./
COPY .git .git
COPY src src
RUN gradle --no-daemon build

FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar ViaProxy.jar
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]