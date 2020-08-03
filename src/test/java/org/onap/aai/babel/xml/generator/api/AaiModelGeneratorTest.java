/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2019 Nokia. All rights reserved.
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

package org.onap.aai.babel.xml.generator.api;


import org.apache.groovy.util.Maps;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Service;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AaiModelGeneratorTest {

    private AaiModelGenerator generator = new AaiModelGenerator();

    @Test
    public void shouldGenerateModelWithCategory() throws XmlArtifactGenerationException, IOException {
        new ArtifactTestUtils().loadWidgetMappings();
        Model model = new Service();
        model.populateModelIdentificationInformation(Maps.of("category", "NST"));


        String generatedXml = generator.generateModelFor(model);

        assertThat(generatedXml).containsSubsequence("    <model-role>NST</model-role>");
    }
}
