FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/babel

# Build up the deployment folder structure
#RUN groupadd aaiadmin -g 1000
#RUN adduser -u 1000 -h /opt/aaihome/aaiadmin -S -D -G aaiadmin -s /bin/bash aaiadmin
WORKDIR $MICRO_HOME

USER root

# Create the aai user
RUN mkdir -p /opt/aaihome $MICRO_HOME /logs && \
    ln -s /logs $MICRO_HOME/logs && \
    chown -R aaiadmin:aaiadmin $MICRO_HOME && \
    chown -R aaiadmin:aaiadmin /logs

COPY --chown=aaiadmin:aaiadmin /maven/babel/ .
# The start script is executable and can be run directly.

ENTRYPOINT ["/bin/bash", "/opt/app/babel/bin/start.sh"]

USER aaiadmin
