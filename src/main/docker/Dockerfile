FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/babel

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
WORKDIR $MICRO_HOME
COPY /maven/babel/ .
RUN chmod 755 bin/* \
    && ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/babel/bin/start.sh"]
