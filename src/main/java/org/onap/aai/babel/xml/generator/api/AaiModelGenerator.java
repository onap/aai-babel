/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
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
package org.onap.aai.babel.xml.generator.api;

import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.ERROR_CATEGORY;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.ERROR_CODE;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.ERROR_DESCRIPTION;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.GENERATOR_ERROR_CODE;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.GENERATOR_ERROR_SERVICE_INSTANTIATION_FAILED;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.GENERATOR_PARTNER_NAME;
import static org.onap.aai.babel.xml.generator.data.GeneratorConstants.PARTNER_NAME;

import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.logging.CategoryLogLevel;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.cl.api.Logger;
import org.slf4j.MDC;

public interface AaiModelGenerator {

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static AaiModelGenerator getInstance() {
        Logger log = LogHelper.INSTANCE;
        try {
            return AaiModelGenerator.class
                    .cast(Class.forName("org.onap.aai.babel.xml.generator.api.AaiModelGeneratorImpl").newInstance());
        } catch (Exception exception) {
            MDC.put(PARTNER_NAME, GENERATOR_PARTNER_NAME);
            MDC.put(ERROR_CATEGORY, CategoryLogLevel.ERROR.name());
            MDC.put(ERROR_CODE, GENERATOR_ERROR_CODE);
            MDC.put(ERROR_DESCRIPTION, GENERATOR_ERROR_SERVICE_INSTANTIATION_FAILED);
            log.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, exception);
        }
        return null;
    }

    public String generateModelFor(Service service);

    public String generateModelFor(Resource resource);
}
