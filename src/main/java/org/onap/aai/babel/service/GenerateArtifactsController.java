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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.babel.service.data.BabelRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Generate artifacts from the specified request content */
@Path("/app")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@FunctionalInterface
@Tag(name = "Babel Services", description = "APIs for generating artifacts from TOSCA models in AAI Babel")
public interface GenerateArtifactsController {

    @POST
    @Path("/generateArtifacts")
    @Operation(summary = "Generate artifacts", description = "Takes a BabelRequest containing TOSCA service model artifacts and generates AAI-compatible artifacts.", responses = {
            @ApiResponse(responseCode = "200", description = "Artifacts generated successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or malformed input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (authentication failure)"),
            @ApiResponse(responseCode = "500", description = "Internal server error during artifact generation")
    })
    Response generateArtifacts(
            @RequestBody(required = true, description = "The BabelRequest containing TOSCA service model artifacts.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BabelRequest.class))) BabelRequest babelRequest)
            throws AAIAuthException;
}
