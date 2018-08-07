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

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;

/**
 * Tests {@link CsarToXmlConverter}.
 */
public class CsarToXmlConverterTest {

    private static final String ARTIFACT_GENERATOR_CONFIG = "artifact-generator.properties";
    private static final String INCORRECT_CSAR_NAME = "the_name_of_the_csar_file.csar";
    private static final String SERVICE_VERSION = "1.0";

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
    }

    private enum CsarTest {
        VALID_CSAR_FILE(
                "service-SdWanServiceTest-csar.csar"
        ),
        MISSING_METADATA_CSAR(
                "service-MissingMetadataTest.csar"
        ),
        NO_YAML_FILES(
                "noYmlFilesArchive.zip"
        ),
        PORT_MIRROR_CSAR(
                "service_PortMirror.csar"
        );

        private String filename;
        private ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();

        CsarTest(String filename) {
            this.filename = filename;
        }

        public String getName() {
            return filename;
        }

        public byte[] getContent() throws IOException {
            return artifactTestUtils.getCompressedArtifact(filename);
        }
    }

    // The class to be tested.
    private CsarToXmlConverter converter;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE,
                new ArtifactTestUtils().getResourcePath(ARTIFACT_GENERATOR_CONFIG));
        converter = new CsarToXmlConverter();
    }

    @After
    public void tearDown() {
        converter = null;
    }

    @Test(expected = NullPointerException.class)
    public void testNullArtifactSupplied() throws CsarConverterException {
        converter.generateXmlFromCsar(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testMissingName() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(CsarTest.VALID_CSAR_FILE.getContent(), null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testMissingVersion() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(CsarTest.VALID_CSAR_FILE.getContent(), INCORRECT_CSAR_NAME, null);
    }

    @Test(expected = CsarConverterException.class)
    public void testNoPayloadExists() throws CsarConverterException {
        converter.generateXmlFromCsar(new byte[0], INCORRECT_CSAR_NAME, SERVICE_VERSION);
    }

    @Test(expected = CsarConverterException.class)
    public void testCsarFileHasNoYmlFiles() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(CsarTest.NO_YAML_FILES.getContent(), "noYmlFilesArchive.zip", SERVICE_VERSION);
    }

    @Test
    public void testArtifactGeneratorConfigMissing()
            throws IOException, XmlArtifactGenerationException, CsarConverterException {
        exception.expect(CsarConverterException.class);
        exception.expectMessage("Cannot generate artifacts. artifactgenerator.config system property not configured");

        // Unset the required system property
        System.clearProperty(ArtifactGeneratorToscaParser.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE);
        converter.generateXmlFromCsar(CsarTest.VALID_CSAR_FILE.getContent(), CsarTest.VALID_CSAR_FILE.getName(),
                SERVICE_VERSION);
    }

    @Test
    public void testServiceMetadataMissing()
            throws IOException, XmlArtifactGenerationException, CsarConverterException {
        converter.generateXmlFromCsar(CsarTest.MISSING_METADATA_CSAR.getContent(),
                CsarTest.MISSING_METADATA_CSAR.getName(), SERVICE_VERSION);
    }

    @Test
    public void generateXmlFromCsar() throws CsarConverterException, IOException, XmlArtifactGenerationException {
        Map<String, String> expectedXmlFiles = createExpectedXmlFiles();
        List<BabelArtifact> generatedArtifacts = converter.generateXmlFromCsar(CsarTest.VALID_CSAR_FILE.getContent(),
                CsarTest.VALID_CSAR_FILE.getName(), SERVICE_VERSION);

        generatedArtifacts
                .forEach(ga -> assertThat("The content of " + ga.getName() + " must match the expected content",
                        ga.getPayload(), matches(expectedXmlFiles.get(ga.getName()))));
    }

    @Test
    public void generatePortMirrorConfigurationModel()
            throws CsarConverterException, IOException, XmlArtifactGenerationException {
        Map<String, String> expectedXmlFiles = createExpectedXmlFiles();
        List<BabelArtifact> generatedArtifacts = converter.generateXmlFromCsar(CsarTest.PORT_MIRROR_CSAR.getContent(),
                CsarTest.PORT_MIRROR_CSAR.getName(), SERVICE_VERSION);

        generatedArtifacts
                .forEach(ga -> assertThat("The content of " + ga.getName() + " must match the expected content",
                        ga.getPayload(), matches(expectedXmlFiles.get(ga.getName()))));
    }

    public Matcher<String> matches(final String expected) {
        return new BaseMatcher<String>() {
            protected String theExpected = expected;

            @Override
            public boolean matches(Object item) {
                return new ArtifactTestUtils().compareXmlStrings((String) item, theExpected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(theExpected.toString());
            }
        };
    }

    private Map<String, String> createExpectedXmlFiles() throws IOException {
        Map<String, String> xmlMap = new HashMap<>();

        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-SD-WAN-Service-Test-service-1.0.xml");
        filesToLoad.add("AAI-SdWanTestVsp..DUMMY..module-0-resource-2.xml");
        filesToLoad.add("AAI-Tunnel_XConnTest-resource-2.0.xml");
        filesToLoad.add("AAI-SD-WAN-Test-VSP-resource-1.0.xml");
        filesToLoad.add("AAI-Port Mirror_Test-service-1.0.xml");
        filesToLoad.add("AAI-Port Mirroring Configuration-resource-35.0.xml");

        for (String filename : filesToLoad) {
            xmlMap.put(filename, new ArtifactTestUtils().loadResourceAsString("generatedXml/" + filename));
        }

        return xmlMap;
    }
}
