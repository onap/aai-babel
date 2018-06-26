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
package org.onap.aai.babel.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.time.StopWatch;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.csar.vnfcatalog.ToscaToCatalogException;
import org.onap.aai.babel.csar.vnfcatalog.VnfVendorImageExtractor;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.logging.LogHelper.StatusCode;
import org.onap.aai.babel.request.RequestHeaders;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.util.RequestValidationException;
import org.onap.aai.babel.util.RequestValidator;
import org.springframework.stereotype.Service;

/** Generate SDC Artifacts by passing in a CSAR payload, Artifact Name and Artifact version */
@Service
public class GenerateArtifactsServiceImpl implements GenerateArtifactsService {
    private static final LogHelper applicationLogger = LogHelper.INSTANCE;

    private AAIMicroServiceAuth aaiMicroServiceAuth;

    /** @param authorization */
    @Inject
    public GenerateArtifactsServiceImpl(final AAIMicroServiceAuth authorization) {
        this.aaiMicroServiceAuth = authorization;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.onap.aai.babel.service.GenerateArtifactsService#generateArtifacts(javax.ws.rs.core.UriInfo,
     * javax.ws.rs.core.HttpHeaders, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    @Override
    public Response generateArtifacts(UriInfo uriInfo, HttpHeaders headers, HttpServletRequest servletRequest,
            String requestBody) {
        applicationLogger.startAudit(headers, servletRequest);
        applicationLogger.info(ApplicationMsgs.BABEL_REQUEST_PAYLOAD,
                "Received request: " + headers.getRequestHeaders() + requestBody);
        applicationLogger.debug(String.format(
                "Received request. UriInfo \"%s\", HttpHeaders \"%s\", ServletRequest \"%s\", Request \"%s\"", uriInfo,
                headers, servletRequest, requestBody));

        // Additional name/value pairs according to EELF guidelines
        applicationLogger.setContextValue("Protocol", "https");
        applicationLogger.setContextValue("Method", "POST");
        applicationLogger.setContextValue("Path", uriInfo.getPath());
        applicationLogger.setContextValue("Query", uriInfo.getPathParameters().toString());

        RequestHeaders requestHeaders = new RequestHeaders(headers);
        applicationLogger.info(ApplicationMsgs.BABEL_REQUEST_PAYLOAD, requestHeaders.toString());

        String requestId = requestHeaders.getCorrelationId();
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            applicationLogger.info(ApplicationMsgs.MISSING_REQUEST_ID, requestId);
            applicationLogger.setContextValue(MdcParameter.REQUEST_ID, requestId);
        }

        Response response;
        try {
            // Get last URI path segment to use for authentication
            List<PathSegment> pathSegments = uriInfo.getPathSegments();
            String lastPathSegment = pathSegments.isEmpty() ? "" : pathSegments.get(pathSegments.size() - 1).getPath();

            boolean authorized = aaiMicroServiceAuth.validateRequest(headers, servletRequest,
                    AAIMicroServiceAuthCore.HTTP_METHODS.POST, lastPathSegment);

            response = authorized ? generateArtifacts(requestBody)
                    : buildResponse(Status.UNAUTHORIZED, "User not authorized to perform the operation.");
        } catch (Exception e) {
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
            applicationLogger.logAuditError(e);
            return buildResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error while processing request. Please check the babel service logs for more details.\n");
        }

        StatusCode statusDescription;
        int statusCode = response.getStatus();
        if (statusCode / 100 == 2) {
            statusDescription = StatusCode.COMPLETE;
        } else {
            statusDescription = StatusCode.ERROR;
        }
        applicationLogger.logAudit(statusDescription, Integer.toString(statusCode),
                Response.Status.fromStatusCode(statusCode).getReasonPhrase(), response.getEntity().toString());

        return response;
    }

    /**
     * Generate XML model artifacts from request body.
     *
     * @param requestBody the request body in JSON format
     * @return response object containing the generated XML models
     */
    protected Response generateArtifacts(String requestBody) {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        Response response;

        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            BabelRequest babelRequest = gson.fromJson(requestBody, BabelRequest.class);
            new RequestValidator().validateRequest(babelRequest);
            byte[] csarFile = Base64.getDecoder().decode(babelRequest.getCsar());

            List<BabelArtifact> babelArtifacts = new CsarToXmlConverter().generateXmlFromCsar(csarFile,
                    babelRequest.getArtifactName(), babelRequest.getArtifactVersion());

            BabelArtifact vendorImageConfiguration = new VnfVendorImageExtractor().extract(csarFile);
            if (vendorImageConfiguration != null) {
                babelArtifacts.add(vendorImageConfiguration);
            }

            response = buildResponse(Status.OK, gson.toJson(babelArtifacts));
        } catch (JsonSyntaxException e) {
            response = processError(ApplicationMsgs.INVALID_REQUEST_JSON, Status.BAD_REQUEST, e, "Malformed request.");
        } catch (CsarConverterException e) {
            response = processError(ApplicationMsgs.INVALID_CSAR_FILE, Status.INTERNAL_SERVER_ERROR, e,
                    "Error converting CSAR artifact to XML model.");
        } catch (ToscaToCatalogException e) {
            response = processError(ApplicationMsgs.PROCESSING_VNF_CATALOG_ERROR, Status.INTERNAL_SERVER_ERROR, e,
                    "Error converting CSAR artifact to VNF catalog.");
        } catch (RequestValidationException e) {
            response =
                    processError(ApplicationMsgs.PROCESS_REQUEST_ERROR, Status.BAD_REQUEST, e, e.getLocalizedMessage());
        } catch (Exception e) {
            response = processError(ApplicationMsgs.PROCESS_REQUEST_ERROR, Status.INTERNAL_SERVER_ERROR, e,
                    "Error while processing request. Please check the babel service logs for more details.\n");
        } finally {
            applicationLogger.logMetrics(stopwatch, LogHelper.getCallerMethodName(0));
        }

        return response;
    }

    private Response processError(ApplicationMsgs applicationMsgs, Status responseStatus, Exception e, String message) {
        applicationLogger.error(applicationMsgs, e);

        return buildResponse(responseStatus, message);
    }

    /**
     * Helper method to create a REST response object.
     *
     * @param status response status code
     * @param entity response payload
     * @return
     */
    private Response buildResponse(Status status, String entity) {
    // @formatter:off
    return Response.status(status).entity(entity).type(MediaType.TEXT_PLAIN).build();
    // @formatter:on
    }
}
