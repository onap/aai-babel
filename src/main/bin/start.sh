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

APP_HOME="${APP_HOME:-/opt/app/babel}"
mkdir -p ${APP_HOME}/logs/gc

if [ -z "${CONFIG_HOME}" ]; then
	echo "CONFIG_HOME must be set in order to start the process"
	exit 1
fi

PROPS="-DAPP_HOME=${APP_HOME}"
PROPS="${PROPS} -DCONFIG_HOME=${CONFIG_HOME}"
PROPS="${PROPS} -Dtosca.mappings.config=${CONFIG_HOME}/tosca-mappings.json"

PROPS="${PROPS} -Dlogging.config=${APP_HOME}/config/logback.xml"

if [ ! -z "$REQUIRE_CLIENT_AUTH" ]; then
   PROPS="$PROPS -Dserver.ssl.client-auth=${REQUIRE_CLIENT_AUTH}"
fi

PROPS="${PROPS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}"
PROPS="${PROPS} -Daaf.cadi.file=${CONFIG_HOME}/cadi.properties"
PROPS="${PROPS} -Daaf.cadi.file=${CONFIG_HOME}/cadi.properties"

JVM_OPTS="${JVM_OPTS} -XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE:-60}"

exec java ${JVM_OPTS} ${PROPS} -jar ${APP_HOME}/babel*.jar
