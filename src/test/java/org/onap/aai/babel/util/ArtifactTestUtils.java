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
package org.onap.aai.babel.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.base.Throwables;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.openecomp.sdc.generator.data.Artifact;

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
                throw Throwables.propagate(e);
            }
        }).collect(Collectors.toSet());

        toscaFiles.forEach(ts -> {
            boolean payloadFound = false;

            String s = bytesToString(ts.getPayload());

            for (String ymlPayload : ymlPayloads) {
                String tscontent = ymlPayload;

                if (s.endsWith(tscontent)) {
                    payloadFound = true;
                    break;
                }
            }
            assertThat("The content of each yml file must match the actual content of the file extracted ("
                    + ts.getName() + ")", payloadFound, is(true));
        });
    }

    public byte[] loadResource(String resourceName) throws IOException {

        return IOUtils.toByteArray(ArtifactTestUtils.class.getClassLoader().getResource(resourceName));
    }

    public String loadResourceAsString(String resourceName) throws IOException {

        InputStream is = ArtifactTestUtils.class.getClassLoader().getResource(resourceName).openStream();

        String result = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));

        return result;

    }

    public String bytesToString(byte[] source) {
        ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(source));

        String result = new BufferedReader(new InputStreamReader(bis)).lines().collect(Collectors.joining("\n"));

        return result;

    }

}
