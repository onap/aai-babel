/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 European Software Marketing Ltd.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.babel.util;

import org.onap.aai.babel.service.data.BabelRequest;

public class RequestValidator {

    private RequestValidator() {}


    /**
     * Validates that the request body contains the required attributes
     * 
     * @param request the request body to validate
     */
    public static void validateRequest(BabelRequest request) throws RequestValidationException {
        if (request.getCsar() == null) {
            throw new RequestValidationException("No csar attribute found in the request body.");
        }

        if (request.getArtifactVersion() == null) {
            throw new RequestValidationException("No artifact version attribute found in the request body.");
        }

        if (request.getArtifactName() == null) {
            throw new RequestValidationException("No artifact name attribute found in the request body.");
        }
    }
}
