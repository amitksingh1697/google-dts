# docker build --build-arg _OTEL_IMAGE_VERSION=stable -f metrics.dockerfile -t connector-ucp-agent:latest .
# docker run --env PROJECT=sai-sandbox -u root -v /etc:/host-etc:ro -v /boot/efi:/boot/efi:ro -v /var/run/:/var/run/:ro --pid=host --network=host -it --rm connector-ucp-agent:latest

ARG _OTEL_IMAGE_VERSION="stable"

FROM gcr.io/ucp-secure/otel-metrics-agent_java:${_OTEL_IMAGE_VERSION} AS agent

FROM us-docker.pkg.dev/artifact-foundry-prod/docker-3p/openjdk:17-slim AS final

# install gettext for envsubst
RUN apt-get update
RUN apt-get install -y gettext-base

COPY --from=agent /configs /configs
COPY --from=agent /opt /opt
COPY --from=agent /otelsvc /opt/otelsvc

COPY metrics-config.yaml /opt/config/env-metrics-config.yaml
CMD ["/bin/bash", "-c", "envsubst < /opt/config/env-metrics-config.yaml > /opt/config/metrics-config.yaml ;/opt/otelsvc --config=/opt/config/metrics-config.yaml"]
