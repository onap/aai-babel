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
package org.onap.aai.babel.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.aai.babel.service.data.BabelRequest;

public class TestRequestValidator {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testMissingArtifactNameExceptionThrown() throws Exception {
        exception.expect(RequestValidationException.class);
        exception.expectMessage("No artifact name attribute found in the request body.");

        BabelRequest request = new BabelRequest();
        request.setCsar("UEsDBBQACAgIAGsrz0oAAAAAAAAAAAAAAAAJAAAAY3Nhci5tZXRhC3Z");
        request.setArtifactVersion("1.0");
        request.setArtifactName(null);
        new RequestValidator().validateRequest(request);
    }

    @Test
    public void testMissingArtifactVersionExceptionThrown() throws Exception {
        exception.expect(RequestValidationException.class);
        exception.expectMessage("No artifact version attribute found in the request body.");

        BabelRequest request = new BabelRequest();
        request.setCsar("UEsDBBQACAgIAGsrz0oAAAAAAAAAAAAAAAAJAAAAY3Nhci5tZXRhC3Z");
        request.setArtifactVersion(null);
        request.setArtifactName("hello");
        new RequestValidator().validateRequest(request);
    }

    @Test
    public void testMissingCsarFile() throws Exception {
        exception.expect(RequestValidationException.class);
        exception.expectMessage("No csar attribute found in the request body.");

        BabelRequest request = new BabelRequest();
        request.setCsar(null);
        request.setArtifactVersion("1.0");
        request.setArtifactName("hello");
        new RequestValidator().validateRequest(request);
    }

}
