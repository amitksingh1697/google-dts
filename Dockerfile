# syntax=docker/dockerfile:1

# This Dockerfile is to build the connector gRPC server. 
# It can build either a JRE based image or a native binary image using GraalVM.
# By default, a JRE one is built.

# It is recommended to have BUILDKIT enabled to speed up build by caching maven artifacts.

# To build JRE based image:
#     DOCKER_BUILDKIT=1 docker buildx build --build-context="gcloud=$HOME/.config/gcloud" -f Dockerfile . -t connector-server:latest

# To build native image:
#     DOCKER_BUILDKIT=1 docker buildx build --build-context="gcloud=$HOME/.config/gcloud" -f Dockerfile --build-arg imageType=native . -t connector-server:latest

ARG imageType=jre
ARG workDir=/opt/connectors
ARG user=google
ARG group=google

# Maven build for connector server
FROM maven:3.8.5-openjdk-17 AS build
ARG workDir
WORKDIR ${workDir}
COPY . .
RUN --mount=from=gcloud,type=bind,source=.,target=/root/.config/gcloud --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -P release

# Stage for building the Java command to run the connector server with standard JRE
FROM openjdk:17-slim AS jre
ARG workDir
WORKDIR ${workDir}/bin
COPY --from=build ${workDir}/target/package/ ${workDir}/
COPY --from=build ${workDir}/connector-library/jdbc/src/main/resources/license.key ${workDir}/.config/license.key
COPY --from=build ${workDir}/connector-server/src/main/resources/logging.config.properties ${workDir}/bin/logging.config.properties
RUN echo "#!/bin/bash\ncd \`dirname \$0\`\nexec \$JAVA_HOME/bin/java \$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED -Djava.util.logging.config.file=logging.config.properties -jar com.google.cloud.connector.connector-server-0.1.0-SNAPSHOT-libexec.jar \$@" > connector_server \
  && chmod 700 connector_server

# Stage for building the native image for the connector server using GraalVM native-image
FROM ghcr.io/graalvm/native-image:22.3.2 AS native
ARG workDir
WORKDIR ${workDir}
COPY --from=build ${workDir}/target/package/ ${workDir}/
COPY --from=build ${workDir}/connector-library/jdbc/src/main/resources/license.key ${workDir}/.config/license.key
RUN native-image \
  --no-fallback \
  --install-exit-handlers \
  --initialize-at-run-time=io.grpc.netty.shaded.io.netty.util.concurrent.AbstractScheduledEventExecutor \
  -jar com.google.cloud.connector.connector-server-0.1.0-SNAPSHOT-libexec.jar \
  connector_server \
  && mkdir -p bin \
  && mv connector_server bin \
  && rm -f * || true

# Final stage for the docker image that will pick either JRE or native image to run the connector server
FROM ${imageType} AS final
ARG workDir
ARG user
ARG group
WORKDIR ${workDir}
ENV LICENSE_KEY_LOCATION=${workDir}/.config/license.key
RUN groupadd -g 1000 ${group} \
  && useradd -m -u 1000 -g 1000 ${user} \
  && chown -R ${user}:${group} ${workDir}
ENTRYPOINT ["bin/connector_server"]
