/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 European Software Marketing Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.babel.logging;

import com.att.eelf.i18n.EELFResourceManager;
import org.onap.aai.cl.eelf.LogMessageEnum;

public enum ApplicationMsgs implements LogMessageEnum {

    DISTRIBUTION_EVENT, //
    MESSAGE_AUDIT, //
    MESSAGE_METRIC, //
    MISSING_REQUEST_ID, //
    PROCESS_REQUEST_ERROR, //
    INVALID_CSAR_FILE, //
    INVALID_REQUEST_JSON, //
    BABEL_REQUEST_PAYLOAD, //
    BABEL_RESPONSE_PAYLOAD, //
    LOAD_PROPERTIES, //
    PROCESSING_VNF_CATALOG_ERROR, //
    TEMP_FILE_ERROR, //
    MISSING_SERVICE_METADATA;

    static {
        EELFResourceManager.loadMessageBundle("babel-logging-resources");
    }
}
