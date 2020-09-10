#!/bin/sh

# ============LICENSE_START=======================================================
# org.onap.aai
# ================================================================================
# Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
# Copyright (c) 2017-2019 European Software Marketing Ltd.
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

# jre-alpine image has $JAVA_HOME set and added to $PATH
# ubuntu image requires to set $JAVA_HOME and add java to $PATH manually
if ( uname -v | grep -i "ubuntu" ); then
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-`dpkg --print-architecture | awk -F- '{ print $NF }'`
    export PATH=${JAVA_HOME}:$PATH
fi

APP_HOME="${APP_HOME:-/opt/app/babel}"

if [ -z "${CONFIG_HOME}" ]; then
	echo "CONFIG_HOME must be set in order to start the process"
	exit 1
fi

if [ -z "${KEY_STORE_PASSWORD}" ]; then
	echo "KEY_STORE_PASSWORD must be set in order to start the process"
	exit 1
fi

PROPS="-DAPP_HOME=${APP_HOME}"
PROPS="${PROPS} -DCONFIG_HOME=${CONFIG_HOME}"
PROPS="${PROPS} -Dtosca.mappings.config=${CONFIG_HOME}/tosca-mappings.json"
PROPS="${PROPS} -DKEY_STORE_PASSWORD=${KEY_STORE_PASSWORD}"
PROPS="${PROPS} -Dlogging.config=${APP_HOME}/config/logback.xml"
if [ ! -z "$REQUIRE_CLIENT_AUTH" ]; then
   PROPS="$PROPS -Dserver.ssl.client-auth=${REQUIRE_CLIENT_AUTH}"
fi

JVM_MAX_HEAP=${MAX_HEAP:-1024}

JARFILE=$(ls ./babel*.jar);

exec java -Xmx${JVM_MAX_HEAP}m ${PROPS} -jar ${APP_HOME}/${JARFILE}
