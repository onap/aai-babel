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

package org.onap.aai.babel.logging;

import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.request.RequestHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingRequestFilter implements ContainerRequestFilter {

    private static final LogHelper applicationLogger = LogHelper.INSTANCE;

    @Autowired
    private HttpHeaders headers;

    @Autowired
    private HttpServletRequest servletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
      UriInfo uriInfo = requestContext.getUriInfo();
      String requestBody = requestContext.getEntityStream().toString();
      applicationLogger.startAudit(headers, servletRequest);
      applicationLogger.info(ApplicationMsgs.BABEL_REQUEST_PAYLOAD,
              "Received request: " + headers.getRequestHeaders() + requestBody);
      applicationLogger.debug(String.format(
              "Received request. UriInfo \"%s\", HttpHeaders \"%s\", ServletRequest \"%s\", Request \"%s\"", uriInfo,
              headers, servletRequest, requestBody));

      // Additional name/value pairs according to EELF guidelines
      applicationLogger.setContextValue("Protocol", "https");
      applicationLogger.setContextValue("Method", requestContext.getMethod());
      applicationLogger.setContextValue("Path", uriInfo.getPath());
      applicationLogger.setContextValue("Query", uriInfo.getPathParameters().toString());

      RequestHeaders requestHeaders = new RequestHeaders(headers);
      applicationLogger.info(ApplicationMsgs.BABEL_REQUEST_PAYLOAD, requestHeaders.toString());

      String requestId = requestHeaders.getCorrelationId();
      if (requestId == null || !isRequestIDValid(requestId)) {
          requestId = UUID.randomUUID().toString();
          applicationLogger.info(ApplicationMsgs.MISSING_REQUEST_ID, requestId);
          applicationLogger.setContextValue(MdcParameter.REQUEST_ID, requestId);
      }

    }

    private boolean isRequestIDValid(String requestId) {
      try {
          UUID.fromString(requestId);
      } catch (IllegalArgumentException e) {
          return false;
      }
      return true;
    }
}
