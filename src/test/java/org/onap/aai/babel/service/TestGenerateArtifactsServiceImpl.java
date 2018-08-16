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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Direct invocation of the generate artifacts service implementation.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/babel-beans.xml"})
public class TestGenerateArtifactsServiceImpl {

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    private static final String ARTIFACT_GENERATOR_CONFIG = "artifact-generator.properties";

    @Inject
    private AAIMicroServiceAuth auth;

    @BeforeClass
    public static void setup() {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE,
                new ArtifactTestUtils().getResourcePath(ARTIFACT_GENERATOR_CONFIG));
    }

    @Test
    public void testGenerateArtifacts() throws Exception {
        Response response = processJsonRequest(getRequestJson("success_request_vnf_catalog.json"));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("response.json")));
    }

    /**
     * No VNF Configuration exists.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateArtifactsWithoutVnfConfiguration() throws Exception {
        final byte[] csarContent = new ArtifactTestUtils().getCompressedArtifact("noVnfConfiguration.csar");

        BabelRequest babelRequest = new BabelRequest();
        babelRequest.setCsar(new String(GeneratorUtil.encode(csarContent)));
        babelRequest.setArtifactVersion("3.0");
        babelRequest.setArtifactName("service-Vscpass-Test");

        Response response = processJsonRequest(new Gson().toJson(babelRequest));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("validNoVnfConfigurationResponse.json")));
    }

    @Test
    public void testInvalidCsarFile() throws URISyntaxException, IOException {
        Response response = processJsonRequest(getRequestJson("invalid_csar_request.json"));
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.getEntity(), is("Error converting CSAR artifact to XML model."));
    }

    @Test
    public void testInvalidJsonFile() throws URISyntaxException, IOException {
        Response response = processJsonRequest(getRequestJson("invalid_json_request.json"));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("Malformed request."));
    }

    @Test
    public void testMissingArtifactName() throws Exception {
        Response response = processJsonRequest(getRequestJson("missing_artifact_name_request.json"));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact name attribute found in the request body."));
    }

    @Test
    public void testMissingArtifactVersion() throws Exception {
        Response response = processJsonRequest(getRequestJson("missing_artifact_version_request.json"));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact version attribute found in the request body."));
    }

    @Test
    public void testMissingCsarFile() throws Exception {
        Response response = processJsonRequest(getRequestJson("missing_csar_request.json"));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No csar attribute found in the request body."));
    }

    /**
     * Create a (mocked) HTTPS request and invoke the Babel generate artifacts API.
     *
     * @param resource path to the incoming JSON request
     * @return the Response from the HTTP API
     * @throws URISyntaxException if the URI cannot be created
     * @throws IOException if the resource cannot be loaded
     */
    private Response processJsonRequest(String jsonString) throws URISyntaxException, IOException {
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getRequestUri()).thenReturn(new URI("/validate")); // NOSONAR (mocked)
        Mockito.when(mockUriInfo.getPath(false)).thenReturn("validate"); // URI prefix is stripped by AJSC routing
        Mockito.when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<String, String>());

        // Create mocked request headers map
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put("X-TransactionId", createSingletonList("transaction-id"));
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));

        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        for (Entry<String, List<String>> entry : headersMap.entrySet()) {
            Mockito.when(headers.getRequestHeader(entry.getKey())).thenReturn(entry.getValue());
        }
        Mockito.when(headers.getRequestHeaders()).thenReturn(headersMap);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setSecure(true);
        servletRequest.setScheme("https");
        servletRequest.setServerPort(9501);
        servletRequest.setServerName("localhost");
        servletRequest.setRequestURI("/services/validation-service/v1/app/validate");

        X509Certificate mockCertificate = Mockito.mock(X509Certificate.class);
        Mockito.when(mockCertificate.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=test, OU=qa, O=Test Ltd, L=London, ST=London, C=GB"));

        servletRequest.setAttribute("javax.servlet.request.X509Certificate", new X509Certificate[] {mockCertificate});
        servletRequest.setAttribute("javax.servlet.request.cipher_suite", "");

        GenerateArtifactsServiceImpl service = new GenerateArtifactsServiceImpl(auth);
        return service.generateArtifacts(mockUriInfo, headers, servletRequest, jsonString);
    }

    private String getRequestJson(String resource) throws IOException, URISyntaxException {
        return new ArtifactTestUtils().getRequestJson(resource);
    }

    private String getResponseJson(String jsonResponse) throws IOException, URISyntaxException {
        return new ArtifactTestUtils().getResponseJson(jsonResponse);
    }

    private List<String> createSingletonList(String listItem) {
        return Collections.<String>singletonList(listItem);
    }

}
