/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 European Software Marketing Ltd.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.babel.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.custommonkey.xmlunit.XMLTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.onap.aai.babel.csar.CsarConverterException;
import org.onap.aai.babel.csar.CsarToXmlConverter;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.ModelGenerator;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xml.sax.SAXException;

/**
 * Tests {@link CsarToXmlConverter}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelGenerator.class)
public class CsarToXmlConverterTest extends XMLTestCase {

    private static final String NAME = "the_name_of_the_csar_file.csar";
    private static final String VERSION = "v1";

    private CsarToXmlConverter converter;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        converter = new CsarToXmlConverter();
        URL url = CsarToXmlConverterTest.class.getClassLoader().getResource("artifact-generator.properties");
        System.setProperty("artifactgenerator.config", url.getPath());
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
        byte[] csarArchive = new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar");
        converter.generateXmlFromCsar(csarArchive, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void generateXmlFromCsar_missingVersion() throws CsarConverterException, IOException {
        byte[] csarArchive = new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar");
        converter.generateXmlFromCsar(csarArchive, NAME, null);
    }

    @Test(expected = CsarConverterException.class)
    public void generateXmlFromCsar_noPayloadExists() throws CsarConverterException {
        converter.generateXmlFromCsar(new byte[0], NAME, VERSION);
    }

    @Test(expected = CsarConverterException.class)
    public void generateXmlFromCsar_csarFileHasNoYmlFiles() throws CsarConverterException, IOException {
        byte[] csarArchive = new ArtifactTestUtils().loadResource("compressedArtifacts/noYmlFilesArchive.zip");
        converter.generateXmlFromCsar(csarArchive, "noYmlFilesArchive.zip", VERSION);
    }

    @Test
    public void generateXmlFromCsar_artifactgenerator_config_systemPropertyNotSet()
            throws IOException, XmlArtifactGenerationException, CsarConverterException {
        exception.expect(CsarConverterException.class);
        exception.expectMessage("Cannot generate artifacts. artifactgenerator.config system property not configured");

        byte[] csarArchive =
                new ArtifactTestUtils().loadResource("compressedArtifacts/service-SdWanServiceTest-csar.csar");

        // Unset the required system property
        System.clearProperty("artifactgenerator.config");
        converter.generateXmlFromCsar(csarArchive, VERSION, "service-SdWanServiceTest-csar.csar");
    }

    @Test
    public void generateXmlFromCsar() throws CsarConverterException, IOException, XmlArtifactGenerationException {
        byte[] csarArchive =
                new ArtifactTestUtils().loadResource("compressedArtifacts/service-SdWanServiceTest-csar.csar");

        Map<String, String> expectedXmlFiles = createExpectedXmlFiles();
        List<BabelArtifact> generatedArtifacts =
                converter.generateXmlFromCsar(csarArchive, VERSION, "service-SdWanServiceTest-csar.csar");

        generatedArtifacts.forEach(ga -> {
            try {

                String x1 = expectedXmlFiles.get(ga.getName());

                String x2 = bytesToString(ga.getPayload());

                assertXMLEqual("The content of " + ga.getName() + " must match the expected content", x1, x2);

            } catch (SAXException | IOException e) {
                fail("There was an Exception parsing the XML: "+e.getMessage());
            }
        });
    }

    public String bytesToString(byte[] source) {
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(source));

        String result = new BufferedReader(new InputStreamReader(bis)).lines().collect(Collectors.joining("\n"));

        return result;

    }

    private Map<String, String> createExpectedXmlFiles() throws IOException {
        Map<String, String> xml = new HashMap<>();

        ArtifactTestUtils utils = new ArtifactTestUtils();

        String[] filesToLoad =
                {"AAI-SD-WAN-Service-Test-service-1.0.xml", "AAI-SdWanTestVsp..DUMMY..module-0-resource-2.xml",
                        "AAI-Tunnel_XConnTest-resource-2.0.xml", "AAI-SD-WAN-Test-VSP-resource-1.0.xml"};

        for (String s : filesToLoad) {
            xml.put(s, utils.loadResourceAsString("generatedXml/" + s));

        }

        return xml;
    }
}
