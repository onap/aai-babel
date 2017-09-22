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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Direct invocation of the generate artifacts service implementation
 *
 */
public class TestGenerateArtifactsServiceImpl {
    
    @BeforeClass
    public static void setup() {
        URL url = TestGenerateArtifactsServiceImpl.class.getClassLoader().getResource("artifact-generator.properties");
        System.setProperty("artifactgenerator.config", url.getPath());
    }
    
    @Test
    public void testGenerateArtifacts() throws Exception {
        String jsonRequest = readstringFromFile("jsonFiles/success_request.json");
        Response response = processJsonRequest(jsonRequest);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(readstringFromFile("response/response.json")));
    }

    
    @Test
    public void testInvalidCsarFile() throws URISyntaxException, IOException{    	
    	 String jsonRequest = readstringFromFile("jsonFiles/invalid_csar_request.json");
         Response response = processJsonRequest(jsonRequest);
         assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())); 
         assertThat(response.getEntity(), is("Error converting CSAR artifact to XML model."));
    }
    
    @Test
    public void testInvalidJsonFile() throws URISyntaxException, IOException{    	
    	 String jsonRequest = readstringFromFile("jsonFiles/invalid_json_request.json");
         Response response = processJsonRequest(jsonRequest);
         assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode())); 
         assertThat(response.getEntity(), is("Malformed request."));
    }
    
    @Test
    public void testMissingArtifactName() throws Exception {
        String jsonRequest = readstringFromFile("jsonFiles/missing_artifact_name_request.json");
        Response response = processJsonRequest(jsonRequest);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact name attribute found in the request body." ));
    }
    
    @Test
    public void testMissingArtifactVersion() throws Exception {
        String jsonRequest = readstringFromFile("jsonFiles/missing_artifact_version_request.json");
        Response response = processJsonRequest(jsonRequest);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact version attribute found in the request body."));
    }
    
    @Test
    public void testMissingCsarFile() throws Exception {
        String jsonRequest = readstringFromFile("jsonFiles/missing_csar_request.json");
        Response response = processJsonRequest(jsonRequest);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No csar attribute found in the request body."));
    }
    

    private Response processJsonRequest(String jsonRequest) {
        GenerateArtifactsServiceImpl service = new GenerateArtifactsServiceImpl(/* No authentiction required */ null);
        return service.generateArtifacts(jsonRequest);
    }

    private String readstringFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.lines(Paths.get(ClassLoader.getSystemResource(resourceFile).toURI()))
                .collect(Collectors.joining());
    }
    
    
}
