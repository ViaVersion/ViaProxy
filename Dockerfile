FROM --platform=$BUILDPLATFORM gradle:jdk22 as builder
WORKDIR /build
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties ./
COPY --chown=gradle:gradle src src
RUN gradle --no-daemon build

FROM --platform=$TARGETPLATFORM eclipse-temurin:22-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar ViaProxy.jar
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]