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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.xml.sax.SAXException;

/**
 * Tests {@link CsarToXmlConverter}.
 */
public class TestCsarToXmlConverter {

    private static final String INCORRECT_CSAR_NAME = "the_name_of_the_csar_file.csar";
    private static final String SERVICE_VERSION = "1.0";

    // The class to be tested.
    private CsarToXmlConverter converter;

    @BeforeEach
    public void setup() {
        new ArtifactTestUtils().setGeneratorSystemProperties();
        converter = new CsarToXmlConverter();
    }

    @AfterEach
    public void tearDown() {
        converter = null;
    }

    @Test
    public void testNullArtifactSupplied() throws CsarConverterException {
        assertThrows(NullPointerException.class, () -> {
            converter.generateXmlFromCsar(null, null, null);
        });
    }

    @Test
    public void testMissingName() throws CsarConverterException, IOException {
        assertThrows(NullPointerException.class, () -> {
            converter.generateXmlFromCsar(CsarTest.SD_WAN_CSAR_FILE.getContent(), null, null);
        });
    }

    @Test
    public void testMissingVersion() throws CsarConverterException, IOException {
        assertThrows(NullPointerException.class, () -> {
            converter.generateXmlFromCsar(CsarTest.SD_WAN_CSAR_FILE.getContent(), INCORRECT_CSAR_NAME, null);
        });
    }

    @Test
    public void testNoPayloadExists() throws CsarConverterException {
        assertThrows(CsarConverterException.class, () -> {
            converter.generateXmlFromCsar(new byte[0], INCORRECT_CSAR_NAME, SERVICE_VERSION);
        });
    }

    @Test
    public void testCsarFileHasNoYmlFiles() throws CsarConverterException, IOException {
        assertThrows(CsarConverterException.class, () -> {
            converter.generateXmlFromCsar(CsarTest.NO_YAML_FILES.getContent(), CsarTest.NO_YAML_FILES.getName(),
                    SERVICE_VERSION);
        });
    }

    /**
     * Test that an Exception is thrown when the Artifact Generator's TOSCA Mappings configuration file is not present.
     *
     * @throws CsarConverterException
     *             if there is an error either extracting the YAML files or generating XML artifacts
     * @throws IOException
     *             if an I/O exception occurs
     */
    @Test
    public void generateXmlFromCsarMappingSystemPropertyNotSet() throws CsarConverterException, IOException {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {

            // Unset the required system property
            System.clearProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
            converter.generateXmlFromCsar(CsarTest.SD_WAN_CSAR_FILE.getContent(), CsarTest.SD_WAN_CSAR_FILE.getName(),
                    SERVICE_VERSION);
        });
        assertTrue(exception.getMessage().contains("Cannot generate artifacts. System property "
                + ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE + " not configured"));
    }

    @Test
    public void testServiceMetadataMissing()
            throws IOException, CsarConverterException {
        assertDoesNotThrow(() -> {
            converter.generateXmlFromCsar(CsarTest.MISSING_METADATA_CSAR.getContent(),
                    CsarTest.MISSING_METADATA_CSAR.getName(), SERVICE_VERSION);
        });
    }

    @Test
    public void generateXmlFromSdWanCsar() throws IOException, CsarConverterException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-SD-WAN-Service-Test-service-1.0.xml");
        filesToLoad.add("AAI-SdWanTestVsp..DUMMY..module-0-resource-2.xml");
        filesToLoad.add("AAI-Tunnel_XConnTest-resource-2.0.xml");
        filesToLoad.add("AAI-SD-WAN-Test-VSP-resource-1.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.SD_WAN_CSAR_FILE);
    }

    @Test
    public void generateXmlFromNetworkCollectionCsar() throws IOException, CsarConverterException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-TEST SVC_1-service-1.0.xml");
        filesToLoad.add("AAI-TEST CR_1-resource-7.0.xml");
        filesToLoad.add("AAI-testcr_1..NetworkCollection..0-resource-1.xml");
        filesToLoad.add("AAI-ExtVL-resource-40.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad),
                CsarTest.NETWORK_COLLECTION_CSAR_FILE);
    }

    @Test
    public void generateXmlFromCosAvpnCsar() throws IOException, CsarConverterException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-COS_AVPN-service-1.0.xml");
        filesToLoad.add("AAI-VLAN Network Receptor Configuration-resource-3.0.xml");
        filesToLoad.add("AAI-VRF Entry Configuration-resource-3.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.COS_AVPN_CSAR_FILE);
    }

    @Test
    public void generateXmlFromRgCollector615Csar() throws IOException, CsarConverterException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-RG_collector_6-15-service-1.0.xml");
        filesToLoad.add("AAI-Network Res 1806-resource-4.0.xml");
        filesToLoad.add("AAI-Network Res 1806..NetworkCollection..0-resource-1.xml");
        filesToLoad.add("AAI-ExtVL-resource-48.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.RG_COLLECTOR_615_CSAR_FILE);
    }

    @Test
    public void generatePortMirrorConfigurationModel()
            throws CsarConverterException, IOException, XmlArtifactGenerationException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-Port Mirror_Test-service-1.0.xml");
        filesToLoad.add("AAI-Port Mirroring Configuration-resource-35.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.PORT_MIRROR_CSAR);
    }

    @Test
    public void generateXmlFromServiceProxyCsar()
            throws CsarConverterException, IOException, XmlArtifactGenerationException {
        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-Grouping Service for Test-service-1.0.xml");
        filesToLoad.add("AAI-groupingservicefortest..ResourceInstanceGroup..0-resource-1.xml");
        filesToLoad.add("AAI-groupingservicefortest..ResourceInstanceGroup..1-resource-1.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.SERVICE_PROXY_CSAR_FILE);
    }


    public void generateChildResourcesCsar()
            throws CsarConverterException, IOException, XmlArtifactGenerationException {

        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-Network Cloud VNF Service MOCK-service-1.0.xml");
        filesToLoad.add("AAI-NetworkCloudVnfMock..NC1_Test_Heat_08152018_base..module-0-resource-1.xml");
        filesToLoad.add("AAI-Network Cloud VNF MOCK-resource-1.0.xml");
        filesToLoad.add("AAI-Fabric Configuration-resource-10.0.xml");
        assertThatGeneratedFilesMatchExpected(createExpectedXmlFiles(filesToLoad), CsarTest.CHILD_RESOURCE_CSAR_FILE);
    }

    /**
     * A Matcher for comparing generated XML Strings with expected XML content.
     *
     * @param expected
     *            the expected XML String
     * @return a new Matcher for comparing XML Strings
     */
    public Matcher<String> matches(final String expected) {
        return new BaseMatcher<String>() {
            protected String theExpected = expected;

            @Override
            public boolean matches(Object item) {
                try {
                    return new ArtifactTestUtils().compareXmlStrings((String) item, theExpected);
                } catch (SAXException | IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(theExpected.toString());
            }
        };
    }

    private Map<String, String> createExpectedXmlFiles(List<String> filesToLoad) throws IOException {
        Map<String, String> xmlMap = new HashMap<>();
        for (String filename : filesToLoad) {
            xmlMap.put(filename, new ArtifactTestUtils().loadResourceAsString("generatedXml/" + filename));
        }
        return xmlMap;
    }

    private void assertThatGeneratedFilesMatchExpected(Map<String, String> expectedXmlFiles, CsarTest csarFile)
            throws CsarConverterException, IOException {
        List<BabelArtifact> generatedArtifacts =
                converter.generateXmlFromCsar(csarFile.getContent(), csarFile.getName(), SERVICE_VERSION);
        assertThat("Incorrect number of files generated", //
                generatedArtifacts.size(), is(equalTo(expectedXmlFiles.size())));
        for (BabelArtifact generated : generatedArtifacts) {
            String fileName = generated.getName();
            String expectedXml = expectedXmlFiles.get(fileName);
            assertThat("Missing expected content for " + generated.getName(), expectedXml, is(not(nullValue())));
            assertThat("The content of " + generated.getName() + " must match the expected content",
                    generated.getPayload(), matches(expectedXml));
        }
    }
}
