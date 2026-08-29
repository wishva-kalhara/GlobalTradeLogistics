# syntax=docker/dockerfile:1

########################
# Build stage
########################
FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /workspace

# Cache dependencies separately from source for faster rebuilds
COPY pom.xml .
COPY app/pom.xml app/pom.xml
COPY core/pom.xml core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY iam-svc/pom.xml iam-svc/pom.xml
RUN mvn -B -q -N dependency:go-offline || true

COPY . .
RUN mvn -B -q clean package -DskipTests

########################
# Runtime stage: GlassFish 7 (Jakarta EE 10) on JDK 11
########################
FROM eclipse-temurin:11-jdk-jammy AS runtime

ENV GLASSFISH_HOME=/opt/glassfish7 \
    PATH=/opt/glassfish7/bin:$PATH

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Tool versions are desired state, not build args — single source of truth
# lives in .desired-state/versions.env.
COPY .desired-state/versions.env /tmp/versions.env

RUN set -a && . /tmp/versions.env && set +a \
    && curl -fSL "https://download.eclipse.org/ee4j/glassfish/glassfish-${GLASSFISH_VERSION}.zip" -o /tmp/glassfish.zip \
    && unzip -q /tmp/glassfish.zip -d /opt \
    && mv /opt/glassfish7 "${GLASSFISH_HOME}" 2>/dev/null || true \
    && rm -f /tmp/glassfish.zip \
    # MySQL JDBC driver (Connector/J), so the app can reach the database container
    && curl -fSL "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_JDBC_VERSION}/mysql-connector-j-${MYSQL_JDBC_VERSION}.jar" \
       -o "${GLASSFISH_HOME}/glassfish/domains/domain1/lib/mysql-jdbc.jar" \
    && rm -f /tmp/versions.env

COPY --from=build /workspace/app/target/*.ear /opt/deploy/global-trade-logistics.ear
COPY .build-new/entrypoint.sh /opt/glassfish7/entrypoint.sh
RUN chmod +x /opt/glassfish7/entrypoint.sh

EXPOSE 8080 8181 4848

ENTRYPOINT ["/opt/glassfish7/entrypoint.sh"]
