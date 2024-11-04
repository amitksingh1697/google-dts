# This Dockerfile is to build the connector gRPC client.
# To build the image run the following command from the parent directory:
# mvn clean package -P release -DskipTests -pl connector-client -am && docker build -f connector-client/Dockerfile . -t connector-client:latest

FROM us-docker.pkg.dev/artifact-foundry-prod/docker-3p/openjdk:17-slim AS final
ARG workDir=/opt/connectors
WORKDIR ${workDir}
COPY connector-client/target/package/ .
RUN echo "#!/bin/bash\ncd \`dirname \$0\`\nexec \$JAVA_HOME/bin/java \$JAVA_OPTS -jar com.google.cloud.connector.connector-client-0.1.0-SNAPSHOT-libexec.jar \$@" > bin/connector_client && chmod 700 bin/connector_client
RUN groupadd -g 1000 google && useradd -m -u 1000 -g 1000 google && chown -R google:google ${workDir}
ENTRYPOINT ["bin/connector_client"]
