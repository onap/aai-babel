FROM ubuntu:14.04

ARG MICRO_HOME=/opt/app/babel
ARG BIN_HOME=$MICRO_HOME/bin
ARG JAR_FILE=babel.jar

RUN apt-get update

# Install and setup java8
RUN apt-get update && apt-get install -y software-properties-common
## sudo -E is required to preserve the environment. If you remove that line, it will most like freeze at this step
RUN sudo -E add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-8-jdk
## Setup JAVA_HOME, this is useful for docker commandline
ENV JAVA_HOME usr/lib/jvm/java-8-openjdk-amd64
RUN export JAVA_HOME

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
COPY ${JAR_FILE} $MICRO_HOME
RUN mkdir -p $BIN_HOME
COPY *.sh $BIN_HOME
RUN chmod 755 $BIN_HOME/*
# Changes related to:AAI-2174
# Change aai babel  container processes to run as non-root on the host

#Note:The group id and user id used below (492382 & 341790 respectively) are chosen arbitarily based on assumption that
# these are not used elsewhere. Please see  https://jira.onap.org/browse/AAI-2172 for more background on this.

RUN mkdir /opt/aaihome && \
     groupadd -g 492382 aaiadmin && \
     useradd -r -u 341790  -g 492382 -ms /bin/sh -d /opt/aaihome/aaiadmin aaiadmin && \
     
      ##The following 2 lines are added to add the user to the sudoers group
      ##The script src\main\bin\start.sh could then optionally run the process as sudo user if an environment variable is set
      ## By default the sudo mode is disabled.
      usermod -aG sudo aaiadmin  &&\
      echo  'aaiadmin  ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers && \      
     chown -R aaiadmin:aaiadmin $MICRO_HOME &&\
     mkdir /logs && \
     chown -R aaiadmin:aaiadmin  /logs

USER aaiadmin
RUN ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/babel/bin/start.sh"]
