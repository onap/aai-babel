/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2024 Deutsche Telekom. All rights reserved.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.BabelApplication;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * End-to-end integration test that boots the full Babel Spring Boot application over HTTP and exercises the
 * {@code generateArtifacts} REST endpoint through the real Jersey/Jetty stack.
 *
 * <p>
 * Unlike {@link TestGenerateArtifactsServiceImpl} (which invokes the service implementation directly with mocked
 * request objects), this test sends an actual HTTP request to the running application, so it covers servlet
 * dispatch, the JAX-RS application path, JSON (de)serialisation over the wire and the authentication filter
 * (authentication is disabled via babel-auth.properties in src/test/resources).
 */
@SpringBootTest(classes = BabelApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "APP_HOME=.",
    "CONFIG_HOME=src/test/resources"
})
public class GenerateArtifactsServiceIT {

    private static final String GENERATE_ARTIFACTS_PATH = "/v1/app/generateArtifacts";

    static {
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    @LocalServerPort
    private int port;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Gson gson;

    @BeforeAll
    public static void setup() {
        new ArtifactTestUtils().setGeneratorSystemProperties();
    }

    /**
     * POST a valid CSAR to the live endpoint and assert that a non-empty list of model artifacts is returned.
     *
     * @throws IOException
     *             if the CSAR test resource cannot be loaded
     */
    @Test
    public void generateArtifactsForValidCsarOverHttp() throws IOException {
        ResponseEntity<String> response = postCsar(CsarTest.VNF_VENDOR_CSAR);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(notNullValue()));

        List<BabelArtifact> artifacts = Arrays.asList(gson.fromJson(response.getBody(), BabelArtifact[].class));
        assertThat("a valid CSAR should yield model artifacts", artifacts, is(not(empty())));
        for (BabelArtifact artifact : artifacts) {
            assertThat("each artifact must have a name", artifact.getName(), is(notNullValue()));
            assertThat("each artifact must have a type", artifact.getType(), is(notNullValue()));
            assertThat("each artifact must have a payload", artifact.getPayload(), is(notNullValue()));
        }
    }

    /**
     * POST a request with no artifact name and assert the endpoint rejects it with HTTP 400 over the wire.
     */
    @Test
    public void rejectRequestWithoutArtifactNameOverHttp() {
        BabelRequest request = new BabelRequest();
        request.setArtifactVersion("1.0");
        request.setCsar("");

        ResponseEntity<String> response = post(gson.toJson(request));

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    private ResponseEntity<String> postCsar(CsarTest csar) throws IOException {
        return post(gson.toJson(csar.getBabelRequest()));
    }

    private ResponseEntity<String> post(String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        String url = "http://localhost:" + port + contextPath + GENERATE_ARTIFACTS_PATH;
        return restTemplate.postForEntity(url, entity, String.class);
    }
}
