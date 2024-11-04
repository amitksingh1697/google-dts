# This Dockerfile is to build the connector gRPC server along with connectors.
# To build the image, run this command from the parent folder:
# mvn clean package -P release -DskipTests && docker build -f connector-server/Dockerfile . -t connector-server:latest

FROM us-docker.pkg.dev/artifact-foundry-prod/docker-3p/openjdk:17-slim AS final
ARG workDir=/opt/connectors
WORKDIR ${workDir}
ENV LICENSE_KEY_LOCATION=${workDir}/.config/license.key
COPY connector-library/jdbc/src/main/resources/license.key ${workDir}/.config/license.key
COPY connector-server/src/main/resources/logging.config.properties ${workDir}/bin/logging.config.properties
COPY target/package/ .
# --add-opens due to gson serialization of java classes done by CDAP (see b/356902592)
RUN echo "#!/bin/bash\ncd \`dirname \$0\`\nexec \$JAVA_HOME/bin/java \$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED -Djava.util.logging.config.file=logging.config.properties -jar com.google.cloud.connector.connector-server-0.1.0-SNAPSHOT-libexec.jar \$@" > bin/connector_server \
  && chmod 700 bin/connector_server
RUN groupadd -g 1000 google && useradd -m -u 1000 -g 1000 google && chown -R google:google ${workDir}
ENTRYPOINT ["bin/connector_server"]
