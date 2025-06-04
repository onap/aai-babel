FROM eclipse-temurin:11-jre-alpine

USER 1000
ARG MICRO_HOME=/opt/app/babel

# Build up the deployment folder structure
#RUN groupadd aaiadmin -g 1000
#RUN adduser -u 1000 -h /opt/aaihome/aaiadmin -S -D -G aaiadmin -s /bin/bash aaiadmin
WORKDIR $MICRO_HOME


COPY --chown=1000:1000 /maven/babel/ .
# The start script is executable and can be run directly.

ENTRYPOINT ["/bin/sh", "/opt/app/babel/bin/start.sh"]
