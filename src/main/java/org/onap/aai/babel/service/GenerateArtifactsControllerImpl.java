/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.aai.babel.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.List;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.time.StopWatch;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.csar.vnfcatalog.ToscaToCatalogException;
import org.onap.aai.babel.csar.vnfcatalog.VnfVendorImageExtractor;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.logging.LogHelper.StatusCode;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.util.RequestValidationException;
import org.onap.aai.babel.util.RequestValidator;
import org.springframework.stereotype.Controller;

/**
 * Generate SDC Artifacts by passing in a CSAR payload, Artifact Name and Artifact version.
 *
 */
@Controller
@RequiredArgsConstructor
public class GenerateArtifactsControllerImpl implements GenerateArtifactsController {

    private static final LogHelper applicationLogger = LogHelper.INSTANCE;
    private final Gson gson;

    @Override
    public Response generateArtifacts(BabelRequest babelRequest) {
        Response response;

        response = generateArtifactsImpl(babelRequest);

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
     * @param requestBody
     *            the request body in JSON format
     * @return response object containing the generated XML models
     */
    protected Response generateArtifactsImpl(BabelRequest babelRequest) {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        Response response;

        try {
            new RequestValidator().validateRequest(babelRequest);
            byte[] csarFile = Base64.getDecoder().decode(babelRequest.getCsar());

            List<BabelArtifact> babelArtifacts = new CsarToXmlConverter().generateXmlFromCsar(csarFile,
                    babelRequest.getArtifactName(), babelRequest.getArtifactVersion());

            BabelArtifact vendorImageConfiguration = new VnfVendorImageExtractor().extract(csarFile);
            if (vendorImageConfiguration != null) {
                babelArtifacts.add(vendorImageConfiguration);
            }

            response = buildResponse(Status.OK, gson.toJson(babelArtifacts));
            applicationLogger.info(ApplicationMsgs.DISTRIBUTION_EVENT,LogHelper.getCallerMethodName(0));
        } catch (JsonSyntaxException e) {
            response = processError(ApplicationMsgs.INVALID_REQUEST_JSON, Status.BAD_REQUEST, e, "Malformed request.");
        } catch (CsarConverterException e) {
            response = processError(ApplicationMsgs.INVALID_CSAR_FILE, Status.INTERNAL_SERVER_ERROR, e,
                    "Error converting CSAR artifact to XML model.");
        } catch (ToscaToCatalogException e) {
            response = processError(ApplicationMsgs.PROCESSING_VNF_CATALOG_ERROR, Status.INTERNAL_SERVER_ERROR, e,
                    "Error converting CSAR artifact to VNF catalog.");
        } catch (RequestValidationException e) {
            response = processError(ApplicationMsgs.PROCESS_REQUEST_ERROR, Status.BAD_REQUEST, //
                    e, e.getLocalizedMessage());
        } finally {
            applicationLogger.debug(stopwatch + LogHelper.getCallerMethodName(0));
        }

        return response;
    }

    private Response processError(ApplicationMsgs applicationMsgs, Status responseStatus, Exception e, String message) {
        applicationLogger.setContextValue(MdcParameter.RESPONSE_CODE, String.valueOf(responseStatus.getStatusCode()));
        applicationLogger.setContextValue(MdcParameter.RESPONSE_DESCRIPTION, responseStatus.getReasonPhrase());
        applicationLogger.error(applicationMsgs, e);
        return buildResponse(responseStatus, message);
    }

    /**
     * Helper method to create a REST response object.
     *
     * @param status
     *            response status code
     * @param entity
     *            response payload
     * @return
     */
    private Response buildResponse(Status status, String entity) {
        return Response.status(status).entity(entity).type(MediaType.APPLICATION_JSON).build();
    }
}
