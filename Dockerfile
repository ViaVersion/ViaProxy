FROM --platform=$BUILDPLATFORM gradle:jdk17 as base
WORKDIR /build
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties ./
COPY --chown=gradle:gradle src src
RUN gradle --no-daemon build

FROM --platform=$TARGETPLATFORM openjdk:17-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar ViaProxy.jar
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]