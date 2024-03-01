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
package org.onap.aai.babel.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.request.RequestHeaders;

/**
 * Tests {@link RequestHeaders}.
 *
 */
public class TestRequestHeaders {

    /**
     * Tests compatibility with the X-ECOMP-* request headers.
     */
    @Test
    public void testEcompHeaders() {
        String transactionId = "transaction-id";
        String serviceInstanceId = "service-instance-id";

        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put(RequestHeaders.HEADER_REQUEST_ID, createSingletonList(transactionId));
        headersMap.put(RequestHeaders.HEADER_SERVICE_INSTANCE_ID, createSingletonList(serviceInstanceId));
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getRequestId(), is(equalTo(transactionId)));
        assertThat(requestHeaders.getInstanceId(), is(equalTo(serviceInstanceId)));
    }

    @Test
    public void testMultipleHeaderValues() {
        String transactionId = "transaction-id";
        String serviceInstanceId = "service-instance-id";

        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put(RequestHeaders.HEADER_REQUEST_ID, Arrays.asList(transactionId, "fred"));
        headersMap.put(RequestHeaders.HEADER_SERVICE_INSTANCE_ID, Arrays.asList(serviceInstanceId, "bob"));

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getRequestId(), is(equalTo(transactionId)));
        assertThat(requestHeaders.getInstanceId(), is(equalTo(serviceInstanceId)));
    }

    @Test
    public void testStandardHeaders() {
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put("X-TransactionId", createSingletonList("transaction-id"));
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getRequestId(), is(nullValue()));
        assertThat(requestHeaders.getInstanceId(), is(nullValue()));
    }

    @Test
    public void testHeadersWithTransactionIdSuffix() {
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put("X-TransactionId", createSingletonList("transaction-id:123"));
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getCorrelationId(), is(equalTo("transaction-id")));
        assertThat(requestHeaders.getInstanceId(), is(nullValue()));
    }

    @Test
    public void testEmptyHeaders() {
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put(RequestHeaders.HEADER_REQUEST_ID, Collections.emptyList());
        headersMap.put(RequestHeaders.HEADER_SERVICE_INSTANCE_ID, Collections.emptyList());

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getRequestId(), is(nullValue()));
        assertThat(requestHeaders.getInstanceId(), is(nullValue()));
    }

    @Test
    public void testNullHeaders() {
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put(RequestHeaders.HEADER_REQUEST_ID, Collections.emptyList());

        HttpHeaders headers = createMockedHeaders(headersMap);
        Mockito.when(headers.getRequestHeader(RequestHeaders.HEADER_SERVICE_INSTANCE_ID)).thenReturn(null);

        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.getRequestId(), is(nullValue()));
        assertThat(requestHeaders.getInstanceId(), is(nullValue()));
    }

    @Test
    public void testToString() {
        String transactionId = "transaction-id";
        String serviceInstanceId = "service-instance-id";

        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put(RequestHeaders.HEADER_REQUEST_ID, createSingletonList(transactionId));
        headersMap.put(RequestHeaders.HEADER_SERVICE_INSTANCE_ID, createSingletonList(serviceInstanceId));

        HttpHeaders headers = createMockedHeaders(headersMap);
        RequestHeaders requestHeaders = new RequestHeaders(headers);
        assertThat(requestHeaders.toString(), is(equalTo(
                "RequestHeaders [requestId=transaction-id, instanceId=service-instance-id, transactionId=null]")));
    }

    private HttpHeaders createMockedHeaders(MultivaluedHashMap<String, String> headersMap) {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        for (Entry<String, List<String>> entry : headersMap.entrySet()) {
            List<String> valuesList = entry.getValue();
            String value = valuesList == null || valuesList.isEmpty() ? null : valuesList.get(0);
            Mockito.when(headers.getHeaderString(entry.getKey())).thenReturn(value);
        }
        Mockito.when(headers.getRequestHeaders()).thenReturn(headersMap);
        return headers;
    }

    private List<String> createSingletonList(String listItem) {
        return Collections.<String>singletonList(listItem);
    }
}
