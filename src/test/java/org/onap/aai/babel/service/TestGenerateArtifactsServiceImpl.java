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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.security.auth.x500.X500Principal;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Direct invocation of the generate artifacts service implementation.
 *
 */
@SpringBootTest
public class TestGenerateArtifactsServiceImpl {

    static {
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    @Autowired
    private Gson gson;

    @BeforeAll
    public static void setup() {
        new ArtifactTestUtils().setGeneratorSystemProperties();
    }

    /**
     * Test with a valid request (and valid CSAR content) by calling the Service implementation directly using a mocked
     * HTTPS request.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifacts() throws URISyntaxException, IOException {
        Response response = processJsonRequest(CsarTest.VNF_VENDOR_CSAR);
        assertThat(response.toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("response.json")));
    }

    /**
     * Test with a valid request that has no Transaction ID header value.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsWithoutRequestId() throws URISyntaxException, IOException {
        Response response = invokeService(CsarTest.VNF_VENDOR_CSAR.getBabelRequest(), Optional.empty());
        assertThat(response.toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("response.json")));
    }

    /**
     * Test with a valid request without Minor Artifact version.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsWithoutMinorArtifactVersion() throws URISyntaxException, IOException {
        Response response = invokeService(CsarTest.VNF_VENDOR_CSAR.getBabelRequestWithArtifactVersion("1"),
        		Optional.of("transaction-id"));
        assertThat(response.toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("response.json")));
    }

    /**
     * Test with a valid request without Minor Artifact version.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsWithInvalidArtifactVersion() throws URISyntaxException, IOException {
        Response response = invokeService(CsarTest.VNF_VENDOR_CSAR.getBabelRequestWithArtifactVersion("a"),
        		Optional.of("transaction-id"));
        assertThat(response.toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("response.json")));
    }


    /**
     * Test with a valid request with Artifact version less than 1.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsWithArtifactVerLessThan1() throws URISyntaxException, IOException {
        Response response = invokeService(CsarTest.VNF_VENDOR_CSAR.getBabelRequestWithArtifactVersion("0.1"),
        		Optional.of("transaction-id"));
        assertThat(response.toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("responseWithVersionLessThan1.json")));
    }


    /**
     * Test with a valid request, using a CSAR file that has no VNF configuration present.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsWithoutVnfConfiguration() throws IOException, URISyntaxException {
        Response response = processJsonRequest(CsarTest.NO_VNF_CONFIG_CSAR);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(getResponseJson("validNoVnfConfigurationResponse.json")));
    }

    /**
     * Test for a valid request with invalid CSAR file content.
     *
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    @Test
    public void testGenerateArtifactsInvalidCsar() throws IOException, URISyntaxException {
        Response response = processJsonRequest(CsarTest.MULTIPLE_VNF_CSAR);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.getEntity().toString(), containsString("VNF catalog"));
    }

    @Test
    public void testInvalidCsarFile() throws URISyntaxException, IOException {
        BabelRequest request = new BabelRequest();
        request.setArtifactName("hello");
        request.setArtifactVersion("1.0");
        request.setCsar("xxxx");
        Response response = invokeService(request);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat(response.getEntity(), is("Error converting CSAR artifact to XML model."));
    }

    @Test
    public void testMissingArtifactName() throws Exception {
        BabelRequest request = new BabelRequest();
        request.setArtifactVersion("1.0");
        request.setCsar("");
        Response response = invokeService(request);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact name attribute found in the request body."));
    }

    @Test
    public void testMissingArtifactVersion() throws Exception {
        BabelRequest request = new BabelRequest();
        request.setArtifactName("hello");
        request.setCsar("");
        Response response = invokeService(request);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No artifact version attribute found in the request body."));
    }

    @Test
    public void testMissingCsarFile() throws Exception {
        BabelRequest request = new BabelRequest();
        request.setArtifactName("test-name");
        request.setArtifactVersion("1.0");
        Response response = invokeService(request);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), is("No csar attribute found in the request body."));
    }

    /**
     * Create a (mocked) HTTPS request and invoke the Babel generate artifacts API.
     *
     * @param csar
     *            test CSAR file
     * @param auth
     *            the auth module
     * @return the Response from the HTTP API
     * @throws URISyntaxException
     *             if the URI cannot be created
     * @throws IOException
     *             if the resource cannot be loaded
     */
    private Response processJsonRequest(CsarTest csar)
            throws URISyntaxException, IOException {
        return invokeService(csar.getBabelRequest(), Optional.of("transaction-id"));
    }

    /**
     * Create a (mocked) HTTPS request and invoke the Babel generate artifacts API.
     *
     * @param jsonString
     *            the JSON request
     * @return the Response from the HTTP API
     * @throws URISyntaxException
     *             if the URI cannot be created
     */
    private Response invokeService(BabelRequest babelRequest) throws URISyntaxException {
        return invokeService(babelRequest, Optional.of("transaction-id"));
    }

    /**
     * Create a (mocked) HTTPS request and invoke the Babel generate artifacts API.
     *
     * @param jsonString
     *            the JSON request
     * @param transactionId
     *            optional X-TransactionId value for the HTTP request
     * @param auth
     *            the auth module
     * @return the Response from the HTTP API
     * @throws URISyntaxException
     *             if the URI cannot be created
     */
    private Response invokeService(BabelRequest babelRequest, Optional<String> transactionId)
            throws URISyntaxException {
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getRequestUri()).thenReturn(new URI("/validate")); // NOSONAR (mocked)
        Mockito.when(mockUriInfo.getPath(false)).thenReturn("validate"); // URI prefix is stripped by AJSC routing
        Mockito.when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<String, String>());

        // Create mocked request headers map
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        if (transactionId.isPresent()) {
            headersMap.put("X-TransactionId", createSingletonList(transactionId.get()));
        }
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));

        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        for (Entry<String, List<String>> entry : headersMap.entrySet()) {
            Mockito.when(headers.getRequestHeader(entry.getKey())).thenReturn(entry.getValue());
            Mockito.when(headers.getHeaderString(entry.getKey())).thenReturn(entry.getValue().get(0));
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

        GenerateArtifactsControllerImpl service = new GenerateArtifactsControllerImpl(gson);
        return service.generateArtifacts(babelRequest);
    }

    private String getResponseJson(String jsonResponse) throws IOException, URISyntaxException {
        return new ArtifactTestUtils().getResponseJson(jsonResponse);
    }

    private List<String> createSingletonList(String listItem) {
        return Collections.<String>singletonList(listItem);
    }

}
