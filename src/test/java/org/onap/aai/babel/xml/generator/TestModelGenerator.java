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

import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.data.Artifact;

/**
 * Direct tests of the @{link ModelGenerator} implementation class (to improve code coverage). Not all methods are
 * tested here. Most use cases are covered by direct tests of @{link CsarToXmlConverter}.
 */
public class TestModelGenerator {

    static {
        System.setProperty("APP_HOME", ".");
    }

    @Before
    public void setup() {
        new ArtifactTestUtils().setGeneratorSystemProperties();
    }

    @Test
    public void testDefaultServiceVersion() throws XmlArtifactGenerationException, IOException {
        Artifact ymlFile = new Artifact(null, null, null, null);
        new ModelGenerator().generateArtifacts(CsarTest.SD_WAN_CSAR_FILE.getContent(),
                Collections.singletonList(ymlFile));
    }


}
