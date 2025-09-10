/**
 * ============LICENSE_START=======================================================
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
package org.onap.aai.babel.csar;

import java.io.Serial;

/**
 * This class represents an exception encountered when attempting to convert the YAML files in a CSAR archive into XML.
 */
public class CsarConverterException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for an instance of this exception with just a message.
     *
     * @param message information about the exception
     */
    public CsarConverterException(String message) {
        super(message);
    }
}
