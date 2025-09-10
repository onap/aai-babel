/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
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

package org.onap.aai.babel.request;

import java.util.Optional;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

/** Bean to represent the ECOMP request/transaction IDs required for EELF logging. */
public class RequestHeaders {

    // ECOMP request ID a.k.a. transaction ID or correlation ID
    public static final String HEADER_REQUEST_ID = "X-ECOMP-RequestID";
    public static final String HEADER_SERVICE_INSTANCE_ID = "X-ECOMP-ServiceInstanceID";
    // This value should match with org.openecomp.restclient.client.Headers.TRANSACTION_ID
    private static final String HEADER_X_TRANSACTION_ID = "X-TransactionId";

    private String requestId;
    private String instanceId;
    private String transactionId;

    public RequestHeaders(HttpHeaders headers) {
        requestId = headers.getHeaderString(RequestHeaders.HEADER_REQUEST_ID);
        instanceId = headers.getHeaderString(RequestHeaders.HEADER_SERVICE_INSTANCE_ID);
        transactionId = headers.getHeaderString(RequestHeaders.HEADER_X_TRANSACTION_ID);
    }
    public RequestHeaders(MultivaluedMap<String, String> headers) {
        requestId = headers.getFirst(RequestHeaders.HEADER_REQUEST_ID);
        instanceId = headers.getFirst(RequestHeaders.HEADER_SERVICE_INSTANCE_ID);
        transactionId = headers.getFirst(RequestHeaders.HEADER_X_TRANSACTION_ID);
    }

    public String getRequestId() {
        return requestId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Get the global request ID from the HTTP headers. The value will be taken from the header "X-ECOMP-RequestID" if
     * this is set, or else the value of "X-TransactionId" (which may be null).
     *
     * <p>
     * If the correlation ID contains the symbol : then this character and any trailing characters are removed. This
     * allows for an incrementing numeric sequence where there are multiple HTTP requests for a single transaction.
     *
     * @return the normalized UUID used for correlating transactions across components, or else null (if no ID is set)
     */
    public String getCorrelationId() {
        // If the request ID is missing, use the transaction ID (if present)
        String uuid = Optional.ofNullable(getRequestId()).orElse(getTransactionId());

        // Normalize the correlation ID by removing any suffix
        if (uuid != null && uuid.contains(":")) {
            uuid = uuid.split(":")[0];
        }

        return uuid;
    }

    @Override
    public String toString() {
        return "RequestHeaders [requestId=" + requestId + ", instanceId=" + instanceId + ", transactionId="
                + transactionId + "]";
    }
}
