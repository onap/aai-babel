/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 European Software Marketing Ltd.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.babel.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.Base64;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.util.RequestValidationException;
import org.onap.aai.babel.util.RequestValidator;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;


/**
 * Generate SDC Artifacts by passing in a CSAR payload, Artifact Name and Artifact version
 */
public class GenerateArtifactsServiceImpl implements GenerateArtifactsService {
    private static Logger applicationLogger = LoggerFactory.getInstance().getLogger(GenerateArtifactsServiceImpl.class);

    private AAIMicroServiceAuth aaiMicroServiceAuth;

    /**
     * @param authorization
     */
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
            String requestBody) throws AAIAuthException {
        applicationLogger.debug("Received request: " + requestBody);

        Response response;
        try {
            boolean authorized = aaiMicroServiceAuth.validateRequest(headers, servletRequest,
                    AAIMicroServiceAuthCore.HTTP_METHODS.POST, uriInfo.getPath(false));

            response = authorized ? generateArtifacts(requestBody)
                    : buildResponse(Status.UNAUTHORIZED, "User not authorized to perform the operation.");
        } catch (AAIAuthException e) {
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
            throw e;
        }

        applicationLogger.debug("Sending response: " + response.getStatus() + " " + response.getEntity().toString());
        return response;
    }


    /**
     * Generate XML model artifacts from request body.
     * 
     * @param requestBody the request body in JSON format
     * @return response object containing the generated XML models
     */
    protected Response generateArtifacts(String requestBody) {
        Response response;

        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            BabelRequest babelRequest = gson.fromJson(requestBody, BabelRequest.class);
            RequestValidator.validateRequest(babelRequest);
            byte[] csarFile = Base64.getDecoder().decode(babelRequest.getCsar());
            List<BabelArtifact> xmlArtifacts = new CsarToXmlConverter().generateXmlFromCsar(csarFile,
                    babelRequest.getArtifactName(), babelRequest.getArtifactVersion());
            response = buildResponse(Status.OK, gson.toJson(xmlArtifacts));

        } catch (JsonSyntaxException e) {
            applicationLogger.error(ApplicationMsgs.INVALID_REQUEST_JSON, e);
            response = buildResponse(Status.BAD_REQUEST, "Malformed request.");
        } catch (CsarConverterException e) {
            applicationLogger.error(ApplicationMsgs.INVALID_CSAR_FILE, e);
            response = buildResponse(Status.INTERNAL_SERVER_ERROR, "Error converting CSAR artifact to XML model.");
        } catch (RequestValidationException e) {
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
            response = buildResponse(Status.BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
            response = buildResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error while processing request. Please check the babel service logs for more details.\n");
        }

        return response;
    }

    /**
     * Helper method to create a REST response object.
     * 
     * @param status response status code
     * @param entity response payload
     * @return
     */
    private Response buildResponse(Status status, String entity) {
        //@formatter:off
        return Response
                .status(status)
                .entity(entity)
                .type(MediaType.TEXT_PLAIN)
                .build();
        //@formatter:on
    }
}
