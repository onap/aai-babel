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

if [ -z "${CONFIG_HOME}" ]; then
	echo "CONFIG_HOME must be set in order to start the process"
	exit 1
fi

if [ -z "${KEY_STORE_PASSWORD}" ]; then
	echo "KEY_STORE_PASSWORD must be set in order to start the process"
	exit 1
fi

# Changes related to:AAI-2174
# Change aai babel  container processes to run as non-root on the host
USER_ID=${LOCAL_USER_ID:-9001}
GROUP_ID=${LOCAL_GROUP_ID:-9001}

if [ $(cat /etc/passwd | grep aaiadmin | wc -l) -eq 0 ]; then

        groupadd aaiadmin -g ${GROUP_ID} || {
                echo "Unable to create the group id for ${GROUP_ID}";
                exit 1;
        }
        useradd --shell=/bin/bash -u ${USER_ID} -g ${GROUP_ID} -o -c "" -m aaiadmin || {
                echo "Unable to create the user id for ${USER_ID}";
                exit 1;
        }
fi;

chown -R aaiadmin:aaiadmin ${APP_HOME}
find ${APP_HOME}  -name "*.sh" -exec chmod +x {} +

gosu aaiadmin ln -s /logs $MICRO_HOME/logs

JAVA_CMD="exec gosu aaiadmin java";
###

PROPS="-DAPP_HOME=${APP_HOME}"
PROPS="${PROPS} -DCONFIG_HOME=${CONFIG_HOME}"
PROPS="${PROPS} -Dtosca.mappings.config=${CONFIG_HOME}/tosca-mappings.json"
PROPS="${PROPS} -DKEY_STORE_PASSWORD=${KEY_STORE_PASSWORD}"
if [ ! -z "$REQUIRE_CLIENT_AUTH" ]; then
   PROPS="$PROPS -Dserver.ssl.client-auth=${REQUIRE_CLIENT_AUTH}"
fi

JVM_MAX_HEAP=${MAX_HEAP:-1024}

${JAVA_CMD}  -Xmx${JVM_MAX_HEAP}m ${PROPS} -jar ${APP_HOME}/babel.jar
