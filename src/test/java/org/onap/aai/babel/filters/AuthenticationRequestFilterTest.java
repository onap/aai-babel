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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationRequestFilterTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AAIMicroServiceAuth authService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        webTestClient = webTestClient.mutate()
                                 .responseTimeout(Duration.ofMillis(300000))
                                 .build();
    }

    @Test
    public void testAuthorizedRequest() throws AAIAuthException {
        // Mocking authService to return true
        when(authService.validateRequest(any(), any(HttpServletRequest.class), any(), anyString())).thenReturn(true);

        webTestClient.post()
                .uri("/v1/app/generateArtifacts")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @Disabled
    public void testUnauthorizedRequest() throws AAIAuthException {
        // Mocking authService to return false
        when(authService.validateRequest(any(), any(HttpServletRequest.class), any(), anyString())).thenReturn(false);

        webTestClient.post()
                .uri("/services/babel-service/v1/app/generateArtifacts")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // @TestConfiguration
    // static class TestConfig {

    //     @Bean
    //     public FilterRegistrationBean<AuthenticationRequestFilter> loggingFilter(AAIMicroServiceAuth authService, HttpServletRequest servletRequest) {
    //         FilterRegistrationBean<AuthenticationRequestFilter> registrationBean = new FilterRegistrationBean<>();

    //         registrationBean.setFilter(new AuthenticationRequestFilter(authService, servletRequest));
    //         registrationBean.addUrlPatterns("/test");

    //         return registrationBean;
    //     }

    //     @Bean
    //     public HttpServletRequest httpServletRequest() {
    //         return new MockHttpServletRequest();
    //     }
    // }

    // @RestController
    // static class TestController {

    //     @GetMapping("/test")
    //     public ResponseEntity<String> testEndpoint() {
    //         return ResponseEntity.ok("Authorized");
    //     }
    // }
}
