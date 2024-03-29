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

package org.onap.aai.babel.xml.generator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.util.Resources;
import org.onap.aai.babel.xml.generator.api.AaiArtifactGenerator;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.model.WidgetType;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;

/**
 * Direct tests of the {@link AaiArtifactGenerator} to improve code coverage.
 */
public class TestAaiArtifactGenerator {

    private ArtifactTestUtils testUtils;

    @BeforeEach
    public void setup() {
        testUtils = new ArtifactTestUtils();
        testUtils.setGeneratorSystemProperties();
    }

    @Test
    public void testParserWithInvalidMappings() throws IOException, InvalidArchiveException {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE,
                new ArtifactTestUtils().getResourcePath(Resources.INVALID_TOSCA_MAPPING_CONFIG));

        GenerationData data = generateArtifactsFromCsarFile();
        assertThat("Number of errors produced " + data.getErrorData(), data.getErrorData().size(), is(equalTo(1)));
        assertThat("Number of resources generated", data.getResultData().size(), is(equalTo(0)));
    }

    @Test
    public void testParserWithCsarFile() throws IOException, InvalidArchiveException {
        GenerationData data = generateArtifactsFromCsarFile();
        assertThat("Number of errors produced " + data.getErrorData(), data.getErrorData().size(), is(equalTo(0)));
        assertThat("Number of resources generated", data.getResultData().size(), is(equalTo(2)));
    }

    /**
     * Test that an Exception is thrown when a Widget Type (such as ALLOTTED_RESOURCE) required by Babel is not present
     * in the WidgetType dynamic enumeration.
     *
     * @throws SdcToscaParserException
     *             if the test CSAR file is invalid
     * @throws IOException
     *             if the widget mappings config cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support processed widget type(s)
     */
    @Test
    public void testParserWithIncompleteMappings()
            throws SdcToscaParserException, IOException, XmlArtifactGenerationException {
        assertThrows(IllegalArgumentException.class, () -> {
            testUtils.loadWidgetMappings();

            AaiArtifactGenerator artifactGenerator = new AaiArtifactGenerator();
            WidgetType.clearElements(); // Remove all WidgetTypes so that the generator fails

            ISdcCsarHelper csarHelper = SdcToscaParserFactory.getInstance()
                    .getSdcCsarHelper(TestAaiArtifactGenerator.class.getClassLoader()
                            .getResource(ArtifactTestUtils.CSAR_INPUTS_FOLDER + CsarTest.VNF_VENDOR_CSAR.getName())
                            .getFile().toString());
            artifactGenerator.generateAllArtifacts("1.0", csarHelper);
        });
    }

    /**
     * Invoke the generator with a sample CSAR file.
     *
     * @return the generated AAI Artifacts
     * @throws InvalidArchiveException
     *             if the test CSAR file is invalid
     * @throws IOException
     *             if there are I/O errors reading the CSAR content
     */
    private GenerationData generateArtifactsFromCsarFile() throws InvalidArchiveException, IOException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AdditionalParams.SERVICE_VERSION.getName(), "1.0");
        return new AaiArtifactGenerator().generateArtifact(CsarTest.VNF_VENDOR_CSAR.getContent(),
                CsarTest.VNF_VENDOR_CSAR.extractArtifacts(), additionalParams);
    }
}
