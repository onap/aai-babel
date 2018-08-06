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
package org.onap.aai.babel.csar.vnfcatalog;

/**
 * This class represents an exception raised when trying to extract VNFCatalog data out of a CSAR file.
 */
public class ToscaToCatalogException extends Exception {

    /** Defaulted */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message and cause
     *
     * @param message Friendly information about the exception encountered
     * @param cause the root cause of the exception
     */
    public ToscaToCatalogException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with message only
     *
     * @param message Friendly information about the exception encountered
     */
    public ToscaToCatalogException(String message) {
        super(message);
    }
}
