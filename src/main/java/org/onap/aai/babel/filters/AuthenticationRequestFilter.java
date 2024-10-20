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

import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore.HTTP_METHODS;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@Provider
@RequiredArgsConstructor
public class AuthenticationRequestFilter implements ContainerRequestFilter {

  private static final LogHelper applicationLogger = LogHelper.INSTANCE;
  private final AAIMicroServiceAuth authService;
  private final HttpServletRequest servletRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    List<PathSegment> pathSegments = requestContext.getUriInfo().getPathSegments();
    String lastPathSegment = pathSegments.isEmpty() ? "" : pathSegments.get(pathSegments.size() - 1).getPath();

    try {
      HTTP_METHODS method = HTTP_METHODS.valueOf(requestContext.getMethod());
      boolean authorized = authService.validateRequest(null, servletRequest,
          method, lastPathSegment);
      if (!authorized) {
        requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
      }
    } catch (Exception e) {
      applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
      applicationLogger.logAuditError(e);
      requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
      // log.warn("Authorization skipped for method: {}", requestContext.getMethod());
    }
  }
}
