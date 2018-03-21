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

import java.io.File;
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
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.GeneratorConstants;

/**
 * Tests {@link CsarToXmlConverter}
 */
public class CsarToXmlConverterTest {

    private static final String ARTIFACT_GENERATOR_CONFIG = "artifact-generator.properties";
    private static final String CSAR_FOLDER = "compressedArtifacts";
    private static final String VALID_CSAR_FILE = "service-SdWanServiceTest-csar.csar";
    private static final String INCORRECT_CSAR_NAME = "the_name_of_the_csar_file.csar";
    private static final String SERVICE_VERSION = "1.0";

    private CsarToXmlConverter converter;

    static {
        if (System.getProperty("AJSC_HOME") == null) {
            System.setProperty("AJSC_HOME", ".");
        }
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private ArtifactTestUtils artifactTestUtils;

    @Before
    public void setup() {
        System.setProperty(GeneratorConstants.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE,
                CsarToXmlConverterTest.class.getClassLoader().getResource(ARTIFACT_GENERATOR_CONFIG).getPath());
        converter = new CsarToXmlConverter();
        artifactTestUtils = new ArtifactTestUtils();
    }

    @After
    public void tearDown() {
        converter = null;
    }

    @Test(expected = NullPointerException.class)
    public void generateXmlFromCsar_nullArtifactSupplied() throws CsarConverterException {
        converter.generateXmlFromCsar(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void generateXmlFromCsar_missingName() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(getCsar(VALID_CSAR_FILE), null, null);
    }

    @Test(expected = NullPointerException.class)
    public void generateXmlFromCsar_missingVersion() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(getCsar(VALID_CSAR_FILE), INCORRECT_CSAR_NAME, null);
    }

    @Test(expected = CsarConverterException.class)
    public void generateXmlFromCsar_noPayloadExists() throws CsarConverterException {
        converter.generateXmlFromCsar(new byte[0], INCORRECT_CSAR_NAME, SERVICE_VERSION);
    }

    @Test(expected = CsarConverterException.class)
    public void generateXmlFromCsar_csarFileHasNoYmlFiles() throws CsarConverterException, IOException {
        converter.generateXmlFromCsar(getCsar("noYmlFilesArchive.zip"), "noYmlFilesArchive.zip", SERVICE_VERSION);
    }

    @Test
    public void generateXmlFromCsar_artifactgenerator_config_systemPropertyNotSet()
            throws IOException, XmlArtifactGenerationException, CsarConverterException {
        exception.expect(CsarConverterException.class);
        exception.expectMessage("Cannot generate artifacts. artifactgenerator.config system property not configured");

        // Unset the required system property
        System.clearProperty(GeneratorConstants.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE);
        converter.generateXmlFromCsar(getCsar(VALID_CSAR_FILE), VALID_CSAR_FILE, SERVICE_VERSION);
    }

    @Test
    public void generateXmlFromCsar() throws CsarConverterException, IOException, XmlArtifactGenerationException {
        Map<String, String> expectedXmlFiles = createExpectedXmlFiles();
        List<BabelArtifact> generatedArtifacts =
                converter.generateXmlFromCsar(getCsar(VALID_CSAR_FILE), VALID_CSAR_FILE, SERVICE_VERSION);

        generatedArtifacts
                .forEach(ga -> assertThat("The content of " + ga.getName() + " must match the expected content",
                        ga.getPayload(), matches(expectedXmlFiles.get(ga.getName()))));
    }

    public Matcher<String> matches(final String expected) {
        return new BaseMatcher<String>() {
            protected String theExpected = expected;

            @Override
            public boolean matches(Object o) {
                return artifactTestUtils.compareXMLStrings((String) o, theExpected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(theExpected.toString());
            }
        };
    }

    private byte[] getCsar(String csarFileName) throws IOException {
        return artifactTestUtils.loadResource(CSAR_FOLDER + File.separator + csarFileName);
    }

    private Map<String, String> createExpectedXmlFiles() throws IOException {
        Map<String, String> xmlMap = new HashMap<>();

        List<String> filesToLoad = new ArrayList<>();
        filesToLoad.add("AAI-SD-WAN-Service-Test-service-1.0.xml");
        filesToLoad.add("AAI-SdWanTestVsp..DUMMY..module-0-resource-2.xml");
        filesToLoad.add("AAI-Tunnel_XConnTest-resource-2.0.xml");
        filesToLoad.add("AAI-SD-WAN-Test-VSP-resource-1.0.xml");

        for (String filename : filesToLoad) {
            xmlMap.put(filename, artifactTestUtils.loadResourceAsString("generatedXml" + File.separator + filename));
        }

        return xmlMap;
    }
}
