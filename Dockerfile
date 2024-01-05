FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /build
RUN apk add git
COPY . .
RUN ./gradlew build

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/ViaProxy-*.jar .
ENTRYPOINT ["java", "-jar", "ViaProxy.jar"]