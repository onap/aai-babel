#!/bin/bash

# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
# Copyright © 2017-2018 European Software Marketing Ltd.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================

AJSC_HOME="${AJSC_HOME-/opt/app/babel/}"
AJSC_CONF_HOME="$AJSC_HOME/bundleconfig"

if [ -z "$CONFIG_HOME" ]; then
    echo "CONFIG_HOME must be set in order to start up the process"
    exit 1
fi

# List of ajsc properties which are exposed for modification at deploy time
declare -a MODIFY_PROP_LIST=("KEY_STORE_PASSWORD"
                             "KEY_MANAGER_PASSWORD"
                             "AJSC_JETTY_ThreadCount_MIN" 
                             "AJSC_JETTY_ThreadCount_MAX"
                             "AJSC_JETTY_BLOCKING_QUEUE_SIZE")
PROP_LIST_LENGTH=${#MODIFY_PROP_LIST[@]}  

for (( i=1; i<${PROP_LIST_LENGTH}+1; i++ ));
do
   PROP_NAME=${MODIFY_PROP_LIST[$i-1]}
   PROP_VALUE=${!PROP_NAME}
   if [ ! -z "$PROP_VALUE" ]; then
      sed -i "s/$PROP_NAME=.*$/$PROP_NAME=$PROP_VALUE/g" $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
   fi
done

# Change the CLASSPATH separator to ; if your O/S is Windows
CLASSPATH="$AJSC_HOME/lib/*"
CLASSPATH="$CLASSPATH:$AJSC_HOME/extJars/"
CLASSPATH="$CLASSPATH:$AJSC_HOME/etc/"

PROPS="-DAJSC_HOME=$AJSC_HOME"
PROPS="$PROPS -DAJSC_CONF_HOME=$AJSC_CONF_HOME"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
PROPS="$PROPS -Dlogback.configurationFile=$AJSC_CONF_HOME/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DAJSC_SHARED_CONFIG=$AJSC_CONF_HOME"
PROPS="$PROPS -DAJSC_SERVICE_NAMESPACE=babel"
PROPS="$PROPS -DAJSC_SERVICE_VERSION=v1"
PROPS="$PROPS -Dserver.port=9516"
PROPS="$PROPS -Dartifactgenerator.config=$CONFIG_HOME/artifact-generator.properties"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

echo $CLASSPATH

exec java -Xmx${JVM_MAX_HEAP}m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// sslport=9516
