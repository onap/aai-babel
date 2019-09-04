FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/babel

# Additional optional steps for Ubuntu
RUN if  ((uname -v | grep -i "ubuntu") || ( cat /etc/*release|grep -i "ubuntu")      ) ; then \
   #####################################
   #if JAVA doesnt exist then install it: 
   ######################################
   if type  java  2>/dev/null; then \ 	
     echo "skipping java installation"; \  	
   else \
     apt-get update && apt-get install -y software-properties-common; \
     ## sudo -E is required to preserve the environment. If you remove that line, it will most like freeze at this step
     sudo -E add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-8-jdk ; \
     ## Setup JAVA_HOME, this is useful for docker commandline
     JAVA_HOME=usr/lib/jvm/java-8-openjdk-amd64; \
     export JAVA_HOME ; \
   fi; \
   ##########################################
   #Install gosu if not present
   ##########################################  
   if type  gosu   2>/dev/null; then \     
     echo "skipping gosu installation"; \
   else \
     set -x ; \
     add-apt-repository -y ppa:tianon/gosu; \ 
     sudo apt-get update; \
     sudo apt-get install gosu; \
   fi;\
fi
# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
WORKDIR $MICRO_HOME
COPY /maven/babel/ .
RUN chmod 755 bin/* \
    && ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/babel/bin/start.sh"]
