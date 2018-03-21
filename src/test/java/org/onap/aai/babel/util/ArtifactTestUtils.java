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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.xml.sax.SAXException;

/**
 * This class provides some utilities to assist with running tests.
 */
public class ArtifactTestUtils {

    public void performYmlAsserts(List<Artifact> toscaFiles, List<String> ymlPayloadsToLoad) {
        assertThat("An unexpected number of yml files have been extracted", toscaFiles.size(),
                is(ymlPayloadsToLoad.size()));

        Set<String> ymlPayloads = ymlPayloadsToLoad.stream().map(s -> {
            try {
                return loadResourceAsString(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());

        compareXMLPayloads(toscaFiles, ymlPayloads);
    }

    /**
     * Compare 2 XML strings to see if they have the same content
     *
     * @param string1
     * @param string2
     * @return true if similar
     */
    public boolean compareXMLStrings(String string1, String string2) {
        boolean similar = false;

        try {
            similar = new Diff(string1, string2).similar();
        } catch (SAXException | IOException e) { // NOSONAR
            similar = true;
        }

        return similar;
    }

    public byte[] loadResource(String resourceName) throws IOException {
        return IOUtils.toByteArray(getResource(resourceName));
    }

    public String loadResourceAsString(String resourceName) throws IOException {
        return IOUtils.toString(getResource(resourceName));
    }

    private void compareXMLPayloads(List<Artifact> toscaFiles, Set<String> ymlPayloads) {
        for (Artifact artifact : toscaFiles) {
            boolean payloadFound = false;
            for (String ymlPayload : ymlPayloads) {

                if (compareXMLStrings(convertToString(artifact.getPayload()), ymlPayload)) {
                    payloadFound = true;
                    break;
                }
            }
            assertThat("The content of each yml file must match the actual content of the file extracted ("
                    + artifact.getName() + ")", payloadFound, is(true));
        }
    }

    private URL getResource(String resourceName) {
        return ArtifactTestUtils.class.getClassLoader().getResource(resourceName);
    }

    private String convertToString(byte[] byteArray) {
        return new String(Base64.getDecoder().decode(byteArray), Charset.defaultCharset());
    }
}
