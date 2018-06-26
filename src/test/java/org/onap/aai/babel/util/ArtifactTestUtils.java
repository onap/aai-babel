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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.xml.sax.SAXException;

/**
 * This class provides some utilities to assist with running tests.
 */
public class ArtifactTestUtils {

    private static final String JSON_REQUESTS_FOLDER = "jsonFiles/";
    private static final String JSON_RESPONSES_FOLDER = "response/";
    private static final String CSAR_INPUTS_FOLDER = "compressedArtifacts/";

    public void performYmlAsserts(List<Artifact> toscaFiles, List<String> ymlPayloadsToLoad) {
        assertThat("An unexpected number of YAML files have been extracted", toscaFiles.size(),
                is(ymlPayloadsToLoad.size()));

        Function<? super String, ? extends String> loadResource = s -> {
            try {
                return loadResourceAsString(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Set<String> ymlPayloads = ymlPayloadsToLoad.stream().map(loadResource).collect(Collectors.toSet());
        compareXmlPayloads(toscaFiles, ymlPayloads);
    }

    /**
     * Compare 2 XML strings to see if they have the same content
     *
     * @param string1
     * @param string2
     * @return true if similar
     */
    public boolean compareXmlStrings(String string1, String string2) {
        boolean similar = false;

        try {
            similar = new Diff(string1, string2).similar();
        } catch (SAXException | IOException e) { // NOSONAR
            similar = true;
        }

        return similar;
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

    public String getResourcePath(String resourceName) {
        return getResource(resourceName).getPath();
    }

    private URL getResource(String resourceName) {
        return ArtifactTestUtils.class.getClassLoader().getResource(resourceName);
    }

    private void compareXmlPayloads(List<Artifact> toscaFiles, Set<String> ymlPayloads) {
        for (Artifact artifact : toscaFiles) {
            boolean payloadFound = false;
            for (String ymlPayload : ymlPayloads) {

                if (compareXmlStrings(convertToString(artifact.getPayload()), ymlPayload)) {
                    payloadFound = true;
                    break;
                }
            }
            assertThat("The content of each YAML file must match the actual content of the file extracted ("
                    + artifact.getName() + ")", payloadFound, is(true));
        }
    }

    private String convertToString(byte[] byteArray) {
        return new String(Base64.getDecoder().decode(byteArray), Charset.defaultCharset());
    }

}
