# syntax=docker/dockerfile:1.7
# ---- Build stage ----
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Optional: trust extra CAs placed in .certs/*.pem (used to bypass corporate
# TLS-inspecting proxies during `mvn` dependency download). Empty in CI/prod.
COPY .certs /tmp/extra-certs
RUN set -e; \
    cd /tmp/extra-certs; \
    for f in *.pem; do \
      [ -f "$f" ] || continue; \
      csplit -z -f "cert-" -b "%02d.pem" "$f" '/-----BEGIN CERTIFICATE-----/' '{*}' >/dev/null; \
      i=0; for c in cert-*.pem; do \
        [ -s "$c" ] || continue; \
        keytool -importcert -noprompt -trustcacerts \
          -alias "extra-ca-$(basename "$f" .pem)-$i" \
          -file "$c" \
          -keystore "$JAVA_HOME/lib/security/cacerts" \
          -storepass changeit; \
        i=$((i+1)); \
      done; \
      rm -f cert-*.pem; \
    done

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

RUN useradd --system --home /app --shell /usr/sbin/nologin spring
COPY --from=build --chown=spring:spring /app/target/*.jar /app/app.jar
USER spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
