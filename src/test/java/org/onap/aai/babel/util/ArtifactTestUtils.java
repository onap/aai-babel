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

package org.onap.aai.babel.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.xml.sax.SAXException;

/**
 * This class provides some utilities to assist with running local unit tests.
 */
public class ArtifactTestUtils {

    private static final String JSON_REQUESTS_FOLDER = "jsonFiles/";
    private static final String JSON_RESPONSES_FOLDER = "response/";
    private static final String CSAR_INPUTS_FOLDER = "compressedArtifacts/";

    /**
     * Initialise System Properties for test configuration files.
     */
    public void setGeneratorSystemProperties() {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE,
                getResourcePath(Resources.ARTIFACT_GENERATOR_CONFIG));

        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE,
                getResourcePath(Resources.TOSCA_MAPPING_CONFIG));
    }

    /**
     * Load the Widget to UUID mappings from the Artifact Generator Properties (resource).
     * 
     * @throws IOException
     *             if the properties file is not loaded
     */
    public void loadWidgetToUuidMappings() throws IOException {
        WidgetConfigurationUtil.setConfig(getResourceAsProperties(Resources.ARTIFACT_GENERATOR_CONFIG));
    }

    /**
     * Specific test method for the YAML Extractor test.
     *
     * @param toscaFiles
     *            files extracted by the YamlExtractor
     * @param ymlPayloadsToLoad
     *            the expected YAML files
     * @throws IOException
     *             if an I/O exception occurs
     */
    public void performYmlAsserts(List<Artifact> toscaFiles, List<String> ymlPayloadsToLoad) throws IOException {
        assertThat("An incorrect number of YAML files have been extracted", toscaFiles.size(),
                is(equalTo(ymlPayloadsToLoad.size())));

        Map<String, String> ymlMap = new HashMap<>();
        for (String filename : ymlPayloadsToLoad) {
            ymlMap.put(filename, loadResourceAsString(filename));
        }

        for (Artifact artifact : toscaFiles) {
            String fileName = artifact.getName().replaceFirst("Definitions/", "ymlFiles/");
            String expectedYaml = ymlMap.get(fileName);
            assertThat("Missing expected content for " + fileName, expectedYaml, is(not(nullValue())));
            assertThat("The content of " + fileName + " must match the expected content",
                    convertToString(artifact.getPayload()).replaceAll("\\r\\n?", "\n"), is(equalTo(expectedYaml)));
        }
    }

    /**
     * Compare two XML strings to see if they have the same content.
     *
     * @param string1
     *            XML content
     * @param string2
     *            XML content
     * @return true if XML content is similar
     * @throws IOException
     *             if an I/O exception occurs
     * @throws SAXException
     *             if the XML parsing fails
     */
    public boolean compareXmlStrings(String string1, String string2) throws SAXException, IOException {
        return new Diff(string1, string2).similar();
    }

    public byte[] getCompressedArtifact(String resourceName) throws IOException {
        return loadResourceBytes(CSAR_INPUTS_FOLDER + resourceName);
    }

    public byte[] loadResourceBytes(String resourceName) throws IOException {
        return IOUtils.toByteArray(getResource(resourceName));
    }

    public String loadResourceAsString(String resourceName) throws IOException {
        return IOUtils.toString(getResource(resourceName), Charset.defaultCharset());
    }

    public String getRequestJson(String resource) throws IOException {
        return loadResourceAsString(JSON_REQUESTS_FOLDER + resource);
    }

    public String getResponseJson(String jsonResponse) throws IOException, URISyntaxException {
        return readstringFromFile(JSON_RESPONSES_FOLDER + jsonResponse);
    }

    public String readstringFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.lines(Paths.get(getResource(resourceFile).toURI())).collect(Collectors.joining());
    }

    /**
     * Create Properties from the content of the named resource (e.g. a file on the classpath).
     * 
     * @param resourceName
     *            the resource name
     * @return Properties loaded from the named resource
     * @throws IOException
     *             if an error occurred when reading from the named resource
     */
    public Properties getResourceAsProperties(String resourceName) throws IOException {
        final Properties properties = new Properties();
        InputStream in = ArtifactTestUtils.class.getClassLoader().getResourceAsStream(resourceName);
        properties.load(in);
        in.close();
        return properties;
    }

    public String getResourcePath(String resourceName) {
        return getResource(resourceName).getPath();
    }

    private URL getResource(String resourceName) {
        return ArtifactTestUtils.class.getClassLoader().getResource(resourceName);
    }

    private String convertToString(byte[] byteArray) {
        return new String(Base64.getDecoder().decode(byteArray), Charset.defaultCharset());
    }

}
