FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/babel

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME $MICRO_HOME/logs

RUN groupadd aaiadmin -g 1000
RUN adduser -u 1000 -h /opt/aaihome/aaiadmin -S -D -G aaiadmin -s /bin/bash aaiadmin
WORKDIR $MICRO_HOME

RUN chown -R aaiadmin:aaiadmin $MICRO_HOME $MICRO_HOME/logs

COPY --chown=aaiadmin:aaiadmin /maven/babel/ .

USER aaiadmin

RUN chmod 755 bin/* \
    && ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/babel/bin/start.sh"]
