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
package org.onap.aai.babel.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.csar.extractor.YamlExtractor;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.api.AaiArtifactGenerator;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.data.GeneratorConstants;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;

/**
 * Direct tests of the Model so as to improve code coverage
 */
public class TestToscaParser {

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        System.setProperty(GeneratorConstants.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE,
                new ArtifactTestUtils().getResourcePath("artifact-generator.properties"));
        InputStream in = TestToscaParser.class.getClassLoader().getResourceAsStream("artifact-generator.properties");
        Properties properties = new Properties();
        properties.load(in);
        in.close();
        WidgetConfigurationUtil.setConfig(properties);
    }

    @Test
    public void testParserWithCsarFile() throws IOException, InvalidArchiveException {
        String csarResourceName = "catalog_csar.csar";
        byte[] csarBytes = new ArtifactTestUtils().getCompressedArtifact(csarResourceName);
        List<Artifact> ymlFiles = new YamlExtractor().extract(csarBytes, csarResourceName, "1.0");

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AdditionalParams.SERVICE_VERSION.getName(), "1.0");

        AaiArtifactGenerator generator = new AaiArtifactGenerator();
        GenerationData data = generator.generateArtifact(csarBytes, ymlFiles, additionalParams);

        assertThat(data.getErrorData().size(), is(equalTo(0)));
        assertThat(data.getResultData().size(), is(equalTo(2)));
    }

}
