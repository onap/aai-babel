/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 European Software Marketing Ltd.
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
package org.onap.aai.babel.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.api.AaiArtifactGenerator;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.GenerationData;

/**
 * Direct tests of the {@link AaiArtifactGenerator} to improve code coverage.
 */
public class TestToscaParser {

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
    }

    @Before
    public void setup() {
        new ArtifactTestUtils().setGeneratorSystemProperties();
    }

    @Test
    public void testParserWithCsarFile() throws IOException, InvalidArchiveException {
        List<Artifact> ymlFiles = CsarTest.VNF_VENDOR_CSAR.extractArtifacts();
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AdditionalParams.SERVICE_VERSION.getName(), "1.0");

        AaiArtifactGenerator generator = new AaiArtifactGenerator();
        GenerationData data =
                generator.generateArtifact(CsarTest.VNF_VENDOR_CSAR.getContent(), ymlFiles, additionalParams);

        assertThat(data.getErrorData().size(), is(equalTo(0)));
        assertThat(data.getResultData().size(), is(equalTo(2)));
    }

}
