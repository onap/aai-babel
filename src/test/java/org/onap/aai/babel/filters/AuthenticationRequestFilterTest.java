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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore.HTTP_METHODS;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AuthenticationRequestFilterTest {

    @Mock
    private AAIMicroServiceAuth authService;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private PathSegment pathSegment;

    private AuthenticationRequestFilter filter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new AuthenticationRequestFilter(authService, servletRequest);

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathSegments()).thenReturn(List.of(pathSegment));
        when(pathSegment.getPath()).thenReturn("some-segment");
    }

    @Test
    public void testAuthorizedRequest() throws IOException, AAIAuthException {
        when(requestContext.getMethod()).thenReturn("GET");
        when(authService.validateRequest(any(), eq(servletRequest), eq(HTTP_METHODS.GET), eq("some-segment")))
                .thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void testUnauthorizedRequest() throws IOException, AAIAuthException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(authService.validateRequest(any(), eq(servletRequest), eq(HTTP_METHODS.POST), eq("some-segment")))
                .thenReturn(false);

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), captor.getValue().getStatus());
    }

    @Test
    public void testExceptionDuringValidation() throws IOException, AAIAuthException {
        when(requestContext.getMethod()).thenReturn("DELETE");
        when(authService.validateRequest(any(), eq(servletRequest), eq(HTTP_METHODS.DELETE), eq("some-segment")))
                .thenThrow(new RuntimeException("Failure"));

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), captor.getValue().getStatus());
    }
}
