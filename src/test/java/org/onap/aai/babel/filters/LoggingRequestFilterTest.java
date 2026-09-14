/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.babel.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.request.RequestHeaders;
import org.slf4j.MDC;

public class LoggingRequestFilterTest {

    @Mock private ContainerRequestContext requestContext;
    @Mock private UriInfo uriInfo;
    @Mock private HttpServletRequest servletRequest;

    private MultivaluedMap<String, String> headers;
    private LoggingRequestFilter filter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        MDC.clear();
        headers = new MultivaluedHashMap<>();
        filter = new LoggingRequestFilter(servletRequest);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getEntityStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(uriInfo.getPath()).thenReturn("app/generateArtifacts");
        when(uriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(servletRequest.getRemoteHost()).thenReturn("test-host");
        when(servletRequest.getRemoteAddr()).thenReturn("192.0.2.1");
    }

    @AfterEach
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void testEelfContextValuesAreSet() throws IOException {
        filter.filter(requestContext);
        assertEquals("https", MDC.get("Protocol"));
        assertEquals("POST", MDC.get("Method"));
        assertEquals("app/generateArtifacts", MDC.get("Path"));
        assertNotNull(MDC.get("Query"));
    }

    @Test
    public void testValidCorrelationIdIsPreserved() throws IOException {
        String requestId = UUID.randomUUID().toString();
        headers.putSingle(RequestHeaders.HEADER_REQUEST_ID, requestId);
        filter.filter(requestContext);
        assertEquals(requestId, MDC.get(MdcParameter.REQUEST_ID.value()));
    }

    @Test
    public void testInvalidCorrelationIdIsReplacedWithGeneratedUuid() throws IOException {
        headers.putSingle(RequestHeaders.HEADER_REQUEST_ID, "not-a-uuid");
        filter.filter(requestContext);
        String generated = MDC.get(MdcParameter.REQUEST_ID.value());
        assertNotEquals("not-a-uuid", generated);
        UUID.fromString(generated);
    }

    @Test
    public void testMissingCorrelationIdIsReplacedWithGeneratedUuid() throws IOException {
        filter.filter(requestContext);
        String generated = MDC.get(MdcParameter.REQUEST_ID.value());
        assertNotEquals("missing-request-id", generated);
        UUID.fromString(generated);
    }
}
